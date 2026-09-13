package com.brain.skill

import java.security.MessageDigest
import java.security.KeyFactory
import java.security.Signature
import java.security.spec.X509EncodedKeySpec
import java.util.Base64
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

data class SkillManifest(
    val id: String,
    val name: String,
    val version: String,
    val description: String,
    val category: String,
    val capabilities: Set<String>,
    val triggers: Set<String> = emptySet(),
    val requiredPermissions: Set<String> = emptySet(),
    val trustLevel: TrustLevel = TrustLevel.UNTRUSTED,
    val enabled: Boolean = true,
    val sourceId: String = "builtin",
    val license: String? = null,
    val contentHash: String? = null,
    val signature: String? = null,
    val signatureKeyId: String? = null,
    val signatureAlgorithm: String? = null
)

enum class TrustLevel { CORE, VERIFIED, COMMUNITY, UNTRUSTED }

data class SkillRecord(
    val manifest: SkillManifest,
    val registeredAt: Instant,
    val revoked: Boolean = false,
    val revocationReason: String? = null
)

/**
 * Catálogo declarativo de Skills. Registrar uma Skill nunca concede autorização;
 * as permissões continuam sendo decididas pelo PolicyBroker no momento da execução.
 */
class SkillRegistry(private val trustedSigningKeys: Map<String, ByteArray> = emptyMap()) {
    private val records = ConcurrentHashMap<String, SkillRecord>()
    private val lock = Any()

    fun register(manifest: SkillManifest, content: String? = null): SkillRecord = synchronized(lock) {
        validate(manifest)
        val calculated = content?.let(::sha256)
        if (manifest.contentHash != null && calculated != null && manifest.contentHash != calculated) {
            throw SecurityException("hash da Skill não corresponde ao conteúdo")
        }
        if (manifest.enabled && manifest.sourceId != "builtin" && !verifySignature(manifest, calculated ?: manifest.contentHash)) {
            throw SecurityException("Skill externa ativa exige assinatura Ed25519 verificável")
        }
        val existing = records[manifest.id]
        if (existing?.revoked == true) throw SecurityException("Skill revogada: ${manifest.id}")
        val record = SkillRecord(manifest.copy(contentHash = manifest.contentHash ?: calculated), Instant.now())
        records[manifest.id] = record
        record
    }

    fun revoke(id: String, reason: String): SkillRecord = synchronized(lock) {
        val current = records[id] ?: throw NoSuchElementException("Skill não encontrada: $id")
        val revoked = current.copy(revoked = true, revocationReason = reason)
        records[id] = revoked
        revoked
    }

    fun get(id: String): SkillRecord? = records[id]

    fun listEnabled(): List<SkillRecord> = records.values
        .filter { it.manifest.enabled && !it.revoked }
        .sortedBy { it.manifest.id }

    fun findForCapability(capability: String): List<SkillRecord> = listEnabled()
        .filter { capability in it.manifest.capabilities }

    fun isUsable(id: String): Boolean = get(id)?.let { it.manifest.enabled && !it.revoked } == true

    private fun validate(manifest: SkillManifest) {
        require(Regex("^[a-z0-9][a-z0-9._-]+$").matches(manifest.id)) { "id de Skill inválido" }
        require(manifest.name.isNotBlank()) { "nome de Skill obrigatório" }
        require(Regex("^\\d+\\.\\d+\\.\\d+$").matches(manifest.version)) { "versão deve ser semver" }
        require(manifest.capabilities.isNotEmpty()) { "Skill deve declarar ao menos uma capability" }
        require(manifest.sourceId.isNotBlank()) { "proveniência da Skill é obrigatória" }
        if (manifest.trustLevel == TrustLevel.UNTRUSTED && manifest.enabled) {
            throw SecurityException("Skill externa não verificada não pode ser ativada")
        }
    }

    private fun sha256(value: String): String = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it) }

    private fun verifySignature(manifest: SkillManifest, contentHash: String?): Boolean {
        if (contentHash == null || manifest.signature.isNullOrBlank() || manifest.signatureKeyId.isNullOrBlank()) return false
        if (manifest.signatureAlgorithm != "Ed25519") return false
        val encodedKey = trustedSigningKeys[manifest.signatureKeyId] ?: return false
        return runCatching {
            val key = KeyFactory.getInstance("Ed25519").generatePublic(X509EncodedKeySpec(encodedKey))
            Signature.getInstance("Ed25519").run {
                initVerify(key)
                update(canonicalPayload(manifest, contentHash).toByteArray(Charsets.UTF_8))
                verify(Base64.getDecoder().decode(manifest.signature))
            }
        }.getOrDefault(false)
    }

    private fun canonicalPayload(manifest: SkillManifest, contentHash: String): String =
        listOf(manifest.id, manifest.version, manifest.sourceId, contentHash).joinToString("|")
}
