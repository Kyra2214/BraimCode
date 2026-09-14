package com.sandbox.app

import android.content.Context
import com.sandbox.resource.LocalModelManifest
import org.json.JSONObject
import java.io.File

object LocalModelManifestLoader {
    fun load(context: Context): LocalModelManifest {
        val json = context.resources.openRawResource(R.raw.local_model_manifest)
            .bufferedReader().use { it.readText() }
        val obj = JSONObject(json)
        val manifest = LocalModelManifest(
            id = obj.getString("id"),
            version = obj.getString("version"),
            format = obj.getString("format"),
            architecture = obj.getString("architecture"),
            url = obj.getString("url"),
            sizeBytes = obj.getLong("sizeBytes"),
            sha256 = obj.getString("sha256"),
            license = obj.getString("license"),
            minAppVersion = obj.getString("minAppVersion")
        )
        require(manifest.url.startsWith("https://")) { "URL da mini-LLM deve usar HTTPS" }
        require(manifest.format == "GGUF") { "Formato de modelo não suportado: ${manifest.format}" }
        require(manifest.sizeBytes > 0L && manifest.sha256.matches(Regex("[0-9a-fA-F]{64}"))) {
            "Manifesto da mini-LLM inválido"
        }

        // O download do modelo continua independente. Quando o modelo já
        // existe e o RootFS está pronto, este é o ponto comum usado pelo
        // chat: garante que o motor llama.cpp esteja instalado e verificado
        // antes de o ViewModel montar a execução no Sandbox.
        val sandboxRoot = File(context.filesDir, "sandbox/rootfs")
        val modelFile = File(context.filesDir, "sandbox/models/${manifest.id}.gguf")
        if (sandboxRoot.isDirectory && modelFile.isFile) {
            LocalLlmSandboxBridge(context).ensureEngine().getOrElse {
                throw IllegalStateException("Não foi possível preparar o motor llama.cpp: ${it.message}", it)
            }
        }
        return manifest
    }
}
