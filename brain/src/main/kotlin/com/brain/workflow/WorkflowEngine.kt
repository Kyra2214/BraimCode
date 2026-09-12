package com.brain.workflow

import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.time.Instant


data class WorkflowNode(
    val id: String,
    val capability: String,
    val dependencies: Set<String> = emptySet(),
    val retryLimit: Int = 1
)

data class WorkflowManifest(
    val id: String,
    val version: String,
    val nodes: List<WorkflowNode>,
    val enabled: Boolean = true
)

data class WorkflowStepResult(
    val nodeId: String,
    val success: Boolean,
    val attempts: Int,
    val output: Map<String, Any?> = emptyMap(),
    val error: String? = null
)

data class WorkflowRunResult(
    val runId: String,
    val idempotencyKey: String,
    val status: WorkflowStatus,
    val steps: List<WorkflowStepResult>,
    val error: String? = null
)

enum class WorkflowStatus { RUNNING, COMPLETED, FAILED }

/**
 * Executor determinístico de workflows. O callback de autorização é obrigatório
 * para que cada capability seja verificada antes da execução; esta classe não
 * substitui o PolicyBroker nem transforma um manifesto em autorização.
 */
class WorkflowEngine(private val stateFile: File? = null) {
    private val lock = Any()
    private val completedRuns = linkedMapOf<String, WorkflowRunResult>()

    init { synchronized(lock) { load() } }

    fun run(
        manifest: WorkflowManifest,
        runId: String,
        idempotencyKey: String,
        authorize: (String) -> Boolean,
        execute: (WorkflowNode, Int) -> WorkflowStepResult
    ): WorkflowRunResult = synchronized(lock) {
        completedRuns[idempotencyKey]?.let { return it }
        validate(manifest)
        if (!manifest.enabled) throw IllegalStateException("workflow desabilitado")
        manifest.nodes.forEach { node ->
            if (!authorize(node.capability)) throw SecurityException("capability não autorizada: ${node.capability}")
        }

        val results = mutableListOf<WorkflowStepResult>()
        val done = mutableSetOf<String>()
        for (node in manifest.nodes) {
            if (!node.dependencies.all { it in done }) {
                return@synchronized persist(WorkflowRunResult(runId, idempotencyKey, WorkflowStatus.FAILED, results, "dependência não concluída: ${node.id}"))
            }
            var finalResult: WorkflowStepResult? = null
            for (attempt in 1..(node.retryLimit + 1)) {
                val result = runCatching { execute(node, attempt) }
                    .getOrElse { WorkflowStepResult(node.id, false, attempt, error = it.message ?: "falha desconhecida") }
                finalResult = result.copy(nodeId = node.id, attempts = attempt)
                if (result.success) break
            }
            val result = finalResult ?: WorkflowStepResult(node.id, false, 0, error = "nenhuma tentativa executada")
            results += result
            if (!result.success) return@synchronized persist(WorkflowRunResult(runId, idempotencyKey, WorkflowStatus.FAILED, results, result.error))
            done += node.id
        }
        persist(WorkflowRunResult(runId, idempotencyKey, WorkflowStatus.COMPLETED, results))
    }

    fun result(idempotencyKey: String): WorkflowRunResult? = synchronized(lock) { completedRuns[idempotencyKey] }

    private fun validate(manifest: WorkflowManifest) {
        require(manifest.id.isNotBlank() && manifest.version.isNotBlank()) { "manifesto inválido" }
        require(manifest.nodes.map { it.id }.toSet().size == manifest.nodes.size) { "ids de nodes duplicados" }
        val ids = manifest.nodes.map { it.id }.toSet()
        require(manifest.nodes.all { it.retryLimit >= 0 && it.dependencies.all(ids::contains) }) { "dependência de workflow inválida" }
        val visiting = mutableSetOf<String>(); val visited = mutableSetOf<String>()
        fun visit(id: String) {
            if (!visited.add(id)) return
            check(visiting.add(id)) { "ciclo no workflow" }
            manifest.nodes.first { it.id == id }.dependencies.forEach(::visit)
            visiting.remove(id)
        }
        manifest.nodes.forEach { visit(it.id) }
    }

    private fun persist(result: WorkflowRunResult): WorkflowRunResult {
        completedRuns[result.idempotencyKey] = result
        stateFile?.let { file ->
            file.parentFile?.mkdirs()
            val json = JSONObject().put("version", 1).put("runId", result.runId)
                .put("idempotencyKey", result.idempotencyKey).put("status", result.status.name)
                .put("error", result.error ?: JSONObject.NULL).put("updatedAt", Instant.now().toString())
                .put("steps", JSONArray(result.steps.map { JSONObject().put("nodeId", it.nodeId).put("success", it.success).put("attempts", it.attempts).put("error", it.error ?: JSONObject.NULL) }))
            file.writeText(json.toString())
        }
        return result
    }

    private fun load() {
        val file = stateFile ?: return
        if (!file.exists()) return
        runCatching {
            val json = JSONObject(file.readText())
            val steps = json.optJSONArray("steps") ?: JSONArray()
            val results = (0 until steps.length()).map { i ->
                val item = steps.getJSONObject(i)
                WorkflowStepResult(item.getString("nodeId"), item.getBoolean("success"), item.getInt("attempts"), error = item.optString("error").takeUnless { it == "null" })
            }
            val status = WorkflowStatus.valueOf(json.getString("status"))
            completedRuns[json.getString("idempotencyKey")] = WorkflowRunResult(json.getString("runId"), json.getString("idempotencyKey"), status, results, json.optString("error").takeUnless { it == "null" })
        }
    }
}
