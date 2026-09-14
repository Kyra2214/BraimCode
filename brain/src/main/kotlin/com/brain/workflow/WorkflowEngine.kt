package com.brain.workflow

import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.time.Instant
import java.util.UUID


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

data class WorkflowLease(val workflowId: String, val owner: String, val fencingToken: String, val expiresAtEpochMs: Long)

/** Persistência simples de lease: um owner ativo impede dois workers simultâneos. */
class WorkflowLeaseStore(private val file: File? = null, private val ttlMs: Long = 60_000) {
    @Synchronized fun acquire(workflowId: String, owner: String): WorkflowLease {
        val current = read()
        check(current == null || current.expiresAtEpochMs <= System.currentTimeMillis() || current.owner == owner) {
            "workflow já possui lease ativo"
        }
        val lease = WorkflowLease(workflowId, owner, UUID.randomUUID().toString(), System.currentTimeMillis() + ttlMs)
        file?.let { it.parentFile?.mkdirs(); it.writeText(JSONObject().put("workflowId", workflowId).put("owner", owner).put("fencingToken", lease.fencingToken).put("expiresAt", lease.expiresAtEpochMs).toString()) }
        return lease
    }

    @Synchronized fun check(lease: WorkflowLease): Boolean {
        val current = read() ?: return false
        return current.fencingToken == lease.fencingToken && current.owner == lease.owner && current.expiresAtEpochMs > System.currentTimeMillis()
    }

    @Synchronized fun release(lease: WorkflowLease) { if (check(lease)) file?.delete() }

    private fun read(): WorkflowLease? = file?.takeIf { it.isFile }?.let { runCatching { JSONObject(it.readText()).let { j -> WorkflowLease(j.getString("workflowId"), j.getString("owner"), j.getString("fencingToken"), j.getLong("expiresAt")) } }.getOrNull() }
}

/** Executor determinístico com autorização por capability e fencing de lease. */
class WorkflowEngine(private val stateFile: File? = null, private val leaseStore: WorkflowLeaseStore? = null) {
    private val lock = Any()
    private val completedRuns = linkedMapOf<String, WorkflowRunResult>()

    init { synchronized(lock) { load() } }

    fun run(manifest: WorkflowManifest, runId: String, idempotencyKey: String, authorize: (String) -> Boolean, execute: (WorkflowNode, Int) -> WorkflowStepResult): WorkflowRunResult = synchronized(lock) {
        completedRuns[idempotencyKey]?.let { return it }
        validate(manifest)
        if (!manifest.enabled) throw IllegalStateException("workflow desabilitado")
        val lease = leaseStore?.acquire(manifest.id, runId)
        try {
            manifest.nodes.forEach { node -> if (!authorize(node.capability)) throw SecurityException("capability não autorizada: ${node.capability}") }
            val results = mutableListOf<WorkflowStepResult>()
            val done = mutableSetOf<String>()
            for (node in manifest.nodes) {
                check(lease == null || leaseStore!!.check(lease)) { "lease/fencing expirado" }
                if (!node.dependencies.all { it in done }) return@synchronized persist(WorkflowRunResult(runId, idempotencyKey, WorkflowStatus.FAILED, results, "dependência não concluída: ${node.id}"))
                var finalResult: WorkflowStepResult? = null
                for (attempt in 1..(node.retryLimit + 1)) {
                    val result = runCatching { execute(node, attempt) }.getOrElse { WorkflowStepResult(node.id, false, attempt, error = it.message ?: "falha desconhecida") }
                    finalResult = result.copy(nodeId = node.id, attempts = attempt)
                    if (result.success) break
                }
                val result = finalResult ?: WorkflowStepResult(node.id, false, 0, error = "nenhuma tentativa executada")
                results += result
                if (!result.success) return@synchronized persist(WorkflowRunResult(runId, idempotencyKey, WorkflowStatus.FAILED, results, result.error))
                done += node.id
            }
            persist(WorkflowRunResult(runId, idempotencyKey, WorkflowStatus.COMPLETED, results))
        } finally { lease?.let { leaseStore?.release(it) } }
    }

    fun result(idempotencyKey: String): WorkflowRunResult? = synchronized(lock) { completedRuns[idempotencyKey] }

    private fun validate(manifest: WorkflowManifest) {
        require(manifest.id.isNotBlank() && manifest.version.isNotBlank()) { "manifesto inválido" }
        require(manifest.nodes.map { it.id }.toSet().size == manifest.nodes.size) { "ids de nodes duplicados" }
        val ids = manifest.nodes.map { it.id }.toSet()
        require(manifest.nodes.all { it.retryLimit >= 0 && it.dependencies.all(ids::contains) }) { "dependência de workflow inválida" }
        val visiting = mutableSetOf<String>(); val visited = mutableSetOf<String>()
        fun visit(id: String) { if (!visited.add(id)) return; check(visiting.add(id)) { "ciclo no workflow" }; manifest.nodes.first { it.id == id }.dependencies.forEach(::visit); visiting.remove(id) }
        manifest.nodes.forEach { visit(it.id) }
    }

    private fun persist(result: WorkflowRunResult): WorkflowRunResult {
        completedRuns[result.idempotencyKey] = result
        stateFile?.let { file -> file.parentFile?.mkdirs(); file.writeText(JSONObject().put("version", 1).put("runId", result.runId).put("idempotencyKey", result.idempotencyKey).put("status", result.status.name).put("error", result.error ?: JSONObject.NULL).put("updatedAt", Instant.now().toString()).put("steps", JSONArray(result.steps.map { JSONObject().put("nodeId", it.nodeId).put("success", it.success).put("attempts", it.attempts).put("error", it.error ?: JSONObject.NULL) })).toString()) }
        return result
    }

    private fun load() {
        val file = stateFile ?: return
        if (!file.exists()) return
        runCatching { val json = JSONObject(file.readText()); val steps = json.optJSONArray("steps") ?: JSONArray(); val results = (0 until steps.length()).map { i -> val item = steps.getJSONObject(i); WorkflowStepResult(item.getString("nodeId"), item.getBoolean("success"), item.getInt("attempts"), error = item.optString("error").takeUnless { it == "null" }) }; val status = WorkflowStatus.valueOf(json.getString("status")); completedRuns[json.getString("idempotencyKey")] = WorkflowRunResult(json.getString("runId"), json.getString("idempotencyKey"), status, results, json.optString("error").takeUnless { it == "null" }) }
    }
}
