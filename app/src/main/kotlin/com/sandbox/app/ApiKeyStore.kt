package com.sandbox.app

import android.content.Context

/**
 * Guarda as chaves de API coladas pelo usuário, uma por provider.
 *
 * Usa SharedPreferences em MODE_PRIVATE (mesmo mecanismo já usado em
 * [com.sandbox.android.AndroidSandboxFactory] para o ID de sessão) — ou
 * seja, protegido pelo sandbox de app do Android (outro app não lê isto
 * sem root), mas SEM uma camada extra de criptografia em disco. Se
 * quiser esse reforço depois, dá pra trocar por
 * androidx.security:security-crypto (EncryptedSharedPreferences) sem
 * mudar a API pública desta classe.
 */
class ApiKeyStore(context: Context) {
    private companion object {
        private const val PREFS_NAME = "api_keys"
    }

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun get(providerId: String): String? = prefs.getString(providerId, null)

    fun save(providerId: String, apiKey: String) {
        if (apiKey.isBlank()) {
            prefs.edit().remove(providerId).apply()
        } else {
            prefs.edit().putString(providerId, apiKey).apply()
        }
    }

    fun clear(providerId: String) {
        prefs.edit().remove(providerId).apply()
    }
}
