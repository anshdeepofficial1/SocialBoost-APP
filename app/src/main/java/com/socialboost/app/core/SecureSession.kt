package com.socialboost.app.core

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** A device-bound encrypted bearer token; never committed, logged or backed up. */
class SecureSession(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("sb_session", Context.MODE_PRIVATE)
    private val alias = "socialboost_auth_aes_v1"

    private fun key(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(alias, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(
            KeyGenParameterSpec.Builder(
                alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            ).setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
        )
        return generator.generateKey()
    }

    fun token(): String? {
        val encoded = prefs.getString("access", null) ?: return null
        return try {
            val raw = Base64.decode(encoded, Base64.NO_WRAP)
            val iv = raw.copyOfRange(0, 12)
            val ciphertext = raw.copyOfRange(12, raw.size)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, iv))
            String(cipher.doFinal(ciphertext), Charsets.UTF_8)
        } catch (_: Exception) {
            clear()
            null
        }
    }

    fun saveToken(token: String) {
        require(token.isNotBlank())
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key())
        val result = cipher.iv + cipher.doFinal(token.toByteArray(Charsets.UTF_8))
        prefs.edit().putString("access", Base64.encodeToString(result, Base64.NO_WRAP)).apply()
    }

    fun clear() { prefs.edit().remove("access").apply() }
}
