package com.sally.modforge.github

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** Keeps GitHub credentials encrypted with an Android Keystore AES key. */
class TokenVault(context: Context) {
    private val prefs = context.getSharedPreferences("modforge_secure", Context.MODE_PRIVATE)
    private val alias = "modforge.github.token.aes"

    fun hasToken(): Boolean = !prefs.getString("token_blob", null).isNullOrBlank()

    @Synchronized
    fun saveToken(token: String) {
        require(token.isNotBlank()) { "Token kosong" }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val encrypted = cipher.doFinal(token.toByteArray(Charsets.UTF_8))
        val iv = cipher.iv
        val bundle = ByteArray(1 + iv.size + encrypted.size)
        bundle[0] = iv.size.toByte()
        System.arraycopy(iv, 0, bundle, 1, iv.size)
        System.arraycopy(encrypted, 0, bundle, 1 + iv.size, encrypted.size)
        prefs.edit().putString("token_blob", Base64.encodeToString(bundle, Base64.NO_WRAP)).apply()
    }

    @Synchronized
    fun readToken(): String? {
        val encoded = prefs.getString("token_blob", null) ?: return null
        return runCatching {
            val bundle = Base64.decode(encoded, Base64.NO_WRAP)
            require(bundle.isNotEmpty())
            val ivSize = bundle[0].toInt() and 0xff
            require(ivSize in 12..16 && bundle.size > 1 + ivSize)
            val iv = bundle.copyOfRange(1, 1 + ivSize)
            val encrypted = bundle.copyOfRange(1 + ivSize, bundle.size)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(128, iv))
            String(cipher.doFinal(encrypted), Charsets.UTF_8)
        }.getOrNull()
    }

    fun saveOAuthClientId(clientId: String) {
        prefs.edit().putString("oauth_client_id", clientId.trim()).apply()
    }

    fun readOAuthClientId(): String = prefs.getString("oauth_client_id", "").orEmpty()

    fun clearToken() {
        prefs.edit().remove("token_blob").apply()
    }

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        val existing = keyStore.getKey(alias, null)
        if (existing is SecretKey) return existing
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(
            KeyGenParameterSpec.Builder(
                alias,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build()
        )
        return generator.generateKey()
    }
}
