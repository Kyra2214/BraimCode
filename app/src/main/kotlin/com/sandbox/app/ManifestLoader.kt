package com.sandbox.app

import android.content.Context
import com.sandbox.resource.RootfsManifest
import org.json.JSONObject

/**
 * Lê `res/raw/rootfs_manifest.json`, empacotado dentro do APK.
 *
 * Isso NÃO é o binário proot (esse é código nativo, tem que vir em
 * jniLibs — ver docs/proot-embedding.md). É só metadado: onde baixar o
 * *conteúdo* do rootfs (dados), qual o hash esperado, etc. Baixar dados
 * sob demanda é a Fase 0.2 e não tem restrição de política de loja.
 *
 * O arquivo bundlado vem com valores de exemplo (`SEU-HOST-OU-CDN`) —
 * isso é intencional, é a mesma pendência já documentada em
 * `docs/roadmap-sandbox-fase0.md` (0.2: "decidir onde hospedar o arquivo
 * de verdade"). Esta função detecta o placeholder e falha com uma
 * mensagem clara, em vez de tentar baixar de uma URL que não existe.
 */
object ManifestLoader {

    private const val PLACEHOLDER_MARKER = "SEU-HOST-OU-CDN"

    fun load(context: Context): RootfsManifest {
        val json = context.resources.openRawResource(R.raw.rootfs_manifest)
            .bufferedReader()
            .use { it.readText() }

        val obj = JSONObject(json)
        val manifest = RootfsManifest(
            version = obj.getString("version"),
            arch = obj.getString("arch"),
            distro = obj.optString("distro", "desconhecida"),
            url = obj.getString("url"),
            sizeBytes = obj.getLong("sizeBytes"),
            sha256 = obj.getString("sha256"),
            minAppVersion = obj.getString("minAppVersion"),
            signature = obj.optString("signature", ""),
            signatureUrl = obj.optString("signatureUrl", ""),
            signatureKeyId = obj.optString("signatureKeyId", ""),
            signatureAlgorithm = obj.optString("signatureAlgorithm", ""),
            signatureRequired = obj.optBoolean("signatureRequired", false)
        )

        check(!manifest.url.contains(PLACEHOLDER_MARKER)) {
            "Manifesto do rootfs ainda não configurado: a URL em " +
                "app/src/main/res/raw/rootfs_manifest.json ainda é o " +
                "placeholder de exemplo. Gere o rootfs com " +
                "rootfs-builder/build.sh, hospede o .tar.gz em algum lugar " +
                "(CDN, release do GitHub) e edite esse arquivo com a URL e " +
                "o hash reais — ver 'Pendente' em " +
                "docs/roadmap-sandbox-fase0.md, seção 0.2."
        }

        return manifest
    }
}
