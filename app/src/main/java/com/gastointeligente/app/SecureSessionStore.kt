package com.gastointeligente.app

import android.content.Context
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import org.json.JSONObject
import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PrivateKey
import java.security.MessageDigest
import java.util.Locale
import java.security.spec.MGF1ParameterSpec
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.OAEPParameterSpec
import javax.crypto.spec.PSource
import javax.crypto.spec.SecretKeySpec

/** Public-key encryption permits token rotation without a second biometric prompt.
 * The private key can unwrap the AES key only through an authenticated CryptoObject. */
internal class SecureSessionStore(context: Context, private val vaultUserId: String? = null) {
    // Shared crypto implementation; each vault has keys/storage independent of login.
    private val vaultScope = vaultUserId?.let {
        require(it.length in 1..128)
        MessageDigest.getInstance("SHA-256").digest(it.toByteArray(Charsets.UTF_8))
            .joinToString("") { byte -> "%02x".format(Locale.ROOT, byte) }
    }
    private val preferences = context.getSharedPreferences(
        vaultScope?.let { "secure-vault-v2-$it" } ?: "secure-session-v1", Context.MODE_PRIVATE)
    private val keys = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
    private val legacyAlias = vaultScope?.let { "gi.vault.biometric.v2.$it" } ?: "gi.session.biometric.v1"
    private val writeAlias get() = if (vaultScope == null && Build.VERSION.SDK_INT >= 30) "gi.session.device.v2" else legacyAlias
    // Existing biometric envelopes remain readable until the next authenticated online save.
    private val alias get() = if (vaultScope == null && preferences.getString("key_alias", null) == "gi.session.device.v2") "gi.session.device.v2" else legacyAlias
    fun allowedAuthenticators(forSave: Boolean = false): Int {
        val device = vaultScope == null && Build.VERSION.SDK_INT >= 30 && (forSave || alias == "gi.session.device.v2")
        return androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG or
            (if (device) androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL else 0)
    }
    private val oaep = OAEPParameterSpec("SHA-256", "MGF1", MGF1ParameterSpec.SHA1, PSource.PSpecified.DEFAULT)
    private fun encode(bytes: ByteArray) = Base64.encodeToString(bytes, Base64.NO_WRAP)
    private fun decode(text: String) = Base64.decode(text, Base64.NO_WRAP)

    fun hasSession() = preferences.contains("envelope") && keys.containsAlias(alias)
    // Email is deliberately kept inside the encrypted envelope, not in metadata.
    fun validatePayload(data: JSONObject) {
        if (vaultUserId == null) {
            require(data.getString("access_token").length in 1..16000)
            require(data.getString("refresh_token").length in 1..4096)
            require(data.getString("userId").length in 1..128)
            require(data.getString("email").length in 1..320)
        } else {
            require(data.getString("userId") == vaultUserId)
            val raw = decode(data.getString("master_key"))
            try { require(raw.size == 32) } finally { raw.fill(0) }
            require(data.length() == 2)
        }
    }

    fun save(data: JSONObject) {
        validatePayload(data)
        val alias = writeAlias
        if (!keys.containsAlias(alias)) {
            val spec = KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_DECRYPT)
                .setKeySize(2048).setDigests(KeyProperties.DIGEST_SHA256)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_RSA_OAEP)
                .setUserAuthenticationRequired(true).setInvalidatedByBiometricEnrollment(true)
            if (Build.VERSION.SDK_INT >= 30) spec.setUserAuthenticationParameters(0, KeyProperties.AUTH_BIOMETRIC_STRONG or
                (if (vaultScope == null) KeyProperties.AUTH_DEVICE_CREDENTIAL else 0))
            KeyPairGenerator.getInstance("RSA", "AndroidKeyStore").apply { initialize(spec.build()) }.generateKeyPair()
        }
        val publicKey = KeyFactory.getInstance("RSA").generatePublic(X509EncodedKeySpec(keys.getCertificate(alias).publicKey.encoded))
        val aesKey = KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()
        val aes = Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.ENCRYPT_MODE, aesKey) }
        val encrypted = aes.doFinal(data.toString().toByteArray(Charsets.UTF_8))
        val wrap = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding").apply { init(Cipher.ENCRYPT_MODE, publicKey, oaep) }
        val envelope = JSONObject().put("key", encode(wrap.doFinal(aesKey.encoded)))
            .put("iv", encode(aes.iv)).put("data", encode(encrypted))
        val editor = preferences.edit().putString("envelope", envelope.toString())
        if (vaultScope == null) editor.putString("key_alias", alias)
        check(editor.commit())
    }

    data class LockedSession(val cipher: Cipher, val envelope: String)
    fun prepareUnlock(): LockedSession {
        val envelope = requireNotNull(preferences.getString("envelope", null))
        val cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding")
        cipher.init(Cipher.DECRYPT_MODE, keys.getKey(alias, null) as PrivateKey, oaep)
        return LockedSession(cipher, envelope)
    }

    fun unlock(locked: LockedSession, authenticatedCipher: Cipher): JSONObject {
        check(authenticatedCipher === locked.cipher)
        check(preferences.getString("envelope", null) == locked.envelope)
        val envelope = JSONObject(locked.envelope)
        val key = authenticatedCipher.doFinal(decode(envelope.getString("key")))
        return try {
            val aes = Cipher.getInstance("AES/GCM/NoPadding")
            aes.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(128, decode(envelope.getString("iv"))))
            JSONObject(String(aes.doFinal(decode(envelope.getString("data"))), Charsets.UTF_8))
        } finally { key.fill(0) }
    }

    fun clear() {
        // Remove the private key first: a failed preference write must not leave usable credentials.
        for (keyAlias in setOf(alias, legacyAlias, writeAlias)) {
            if (keys.containsAlias(keyAlias)) keys.deleteEntry(keyAlias)
        }
        check(preferences.edit().clear().commit())
    }
}
