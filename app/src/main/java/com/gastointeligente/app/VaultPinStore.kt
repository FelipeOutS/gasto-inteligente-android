package com.gastointeligente.app

import android.content.Context
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import org.json.JSONObject
import java.security.KeyStore
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Locale
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.Mac
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * PIN local seguro do Cofre Pessoal.
 *
 * Características:
 * - exatamente 6 dígitos;
 * - vinculado a este aparelho;
 * - depende de uma chave não exportável do Android Keystore;
 * - envelope da Master Key fica somente no armazenamento privado do app;
 * - 5 tentativas incorretas bloqueiam o PIN por 15 minutos;
 * - não interfere no PIN legado enquanto a migração não for concluída.
 */
internal class VaultPinStore(
    context: Context,
    userId: String,
) {

    companion object {
        private const val VERSION = 1

        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val HMAC_ALGORITHM = "HmacSHA256"
        private const val AES_TRANSFORMATION = "AES/GCM/NoPadding"

        private const val ENVELOPE_KEY = "envelope"
        private const val CANDIDATE_KEY = "candidate"

        private const val FAILED_ATTEMPTS_KEY = "failed_attempts"
        private const val LOCKED_UNTIL_KEY = "locked_until"

        private const val MAX_FAILED_ATTEMPTS = 5

        private const val LOCKOUT_MS =
            15L * 60L * 1000L

        private val DOMAIN =
            "gasto-inteligente:vault-pin:v1"
                .toByteArray(Charsets.UTF_8)
    }

    data class Status(
        val configured: Boolean,
        val failedAttempts: Int,
        val lockedUntil: Long?,
    )

    private val scope: String

    private val keys =
        KeyStore.getInstance(
            ANDROID_KEYSTORE,
        ).apply {
            load(null)
        }

    private val alias: String

    private val prefs: android.content.SharedPreferences

    init {
        require(userId.length in 1..128) {
            "Identificador de usuário inválido."
        }

        scope =
            MessageDigest
                .getInstance("SHA-256")
                .digest(
                    userId.toByteArray(
                        Charsets.UTF_8,
                    ),
                )
                .joinToString("") { byte ->
                    "%02x".format(
                        Locale.ROOT,
                        byte,
                    )
                }

        alias =
            "gi.vault.pin.v1.$scope"

        prefs =
            context.getSharedPreferences(
                "secure-vault-pin-v1-$scope",
                Context.MODE_PRIVATE,
            )
    }

    private fun encode(
        bytes: ByteArray,
    ): String =
        Base64.encodeToString(
            bytes,
            Base64.NO_WRAP,
        )

    private fun decode(
        text: String,
    ): ByteArray =
        Base64.decode(
            text,
            Base64.NO_WRAP,
        )

    private fun validatePin(
        pin: String,
    ) {
        require(
            pin.matches(
                Regex("^\\d{6}$"),
            ),
        ) {
            "O PIN deve conter exatamente 6 dígitos."
        }
    }

    /**
     * Cria um segredo não exportável exclusivo deste aparelho.
     */
    private fun ensureDeviceKey() {
        if (keys.containsAlias(alias)) {
            return
        }

        val spec =
            KeyGenParameterSpec.Builder(
                alias,
                KeyProperties.PURPOSE_SIGN,
            )
                .setDigests(
                    KeyProperties.DIGEST_SHA256,
                )

        if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.P
        ) {
            spec.setUnlockedDeviceRequired(
                true,
            )
        }

        KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_HMAC_SHA256,
            ANDROID_KEYSTORE,
        ).apply {
            init(
                spec.build(),
            )
        }.generateKey()
    }

    /**
     * PIN + salt + segredo do dispositivo.
     *
     * A chave do Android Keystore nunca é exportada.
     */
    private fun derivePinKey(
        pin: String,
        salt: ByteArray,
    ): ByteArray {

        validatePin(pin)

        val deviceKey =
            keys.getKey(
                alias,
                null,
            )
                ?: error(
                    "A chave segura deste dispositivo não está disponível.",
                )

        val mac =
            Mac.getInstance(
                HMAC_ALGORITHM,
            )

        mac.init(
            deviceKey,
        )

        mac.update(
            DOMAIN,
        )

        mac.update(
            0.toByte(),
        )

        mac.update(
            salt,
        )

        mac.update(
            0.toByte(),
        )

        val pinBytes =
            pin.toByteArray(
                Charsets.UTF_8,
            )

        return try {
            mac.doFinal(
                pinBytes,
            )
        } finally {
            pinBytes.fill(0)
        }
    }

    fun hasPin(): Boolean =
        prefs.contains(
            ENVELOPE_KEY,
        ) &&
                keys.containsAlias(
                    alias,
                )

    /**
     * Retorna o estado do PIN local.
     */
    fun status(): Status {

        normalizeExpiredLock()

        val lockedUntil =
            prefs.getLong(
                LOCKED_UNTIL_KEY,
                0L,
            )
                .takeIf {
                    it > System.currentTimeMillis()
                }

        return Status(
            configured = hasPin(),
            failedAttempts =
                prefs.getInt(
                    FAILED_ATTEMPTS_KEY,
                    0,
                ),
            lockedUntil = lockedUntil,
        )
    }

    /**
     * Se o bloqueio de 15 minutos já terminou,
     * começa um novo ciclo de tentativas.
     */
    private fun normalizeExpiredLock() {

        val lockedUntil =
            prefs.getLong(
                LOCKED_UNTIL_KEY,
                0L,
            )

        if (
            lockedUntil > 0L &&
            lockedUntil <= System.currentTimeMillis()
        ) {
            prefs.edit()
                .remove(
                    LOCKED_UNTIL_KEY,
                )
                .putInt(
                    FAILED_ATTEMPTS_KEY,
                    0,
                )
                .commit()
        }
    }

    private fun ensureNotLocked() {

        normalizeExpiredLock()

        val lockedUntil =
            prefs.getLong(
                LOCKED_UNTIL_KEY,
                0L,
            )

        if (
            lockedUntil >
            System.currentTimeMillis()
        ) {
            val remaining =
                lockedUntil -
                        System.currentTimeMillis()

            val minutes =
                maxOf(
                    1L,
                    (remaining + 59_999L) /
                            60_000L,
                )

            error(
                "Muitas tentativas. PIN bloqueado por $minutes min.",
            )
        }
    }

    private fun registerFailedAttempt() {

        val attempts =
            prefs.getInt(
                FAILED_ATTEMPTS_KEY,
                0,
            ) + 1

        val editor =
            prefs.edit()
                .putInt(
                    FAILED_ATTEMPTS_KEY,
                    attempts,
                )

        if (
            attempts >=
            MAX_FAILED_ATTEMPTS
        ) {
            editor.putLong(
                LOCKED_UNTIL_KEY,
                System.currentTimeMillis() +
                        LOCKOUT_MS,
            )
        }

        check(
            editor.commit(),
        ) {
            "Não foi possível atualizar a proteção do PIN."
        }
    }

    private fun resetFailedAttempts() {

        check(
            prefs.edit()
                .putInt(
                    FAILED_ATTEMPTS_KEY,
                    0,
                )
                .remove(
                    LOCKED_UNTIL_KEY,
                )
                .commit(),
        )
    }

    /**
     * Cria um novo PIN local.
     *
     * Primeiro cria um candidato, testa o round-trip
     * e só depois o transforma no envelope oficial.
     */
    fun enroll(
        pin: String,
        masterKeyBase64: String,
    ) {

        validatePin(
            pin,
        )

        val masterKey =
            decode(
                masterKeyBase64,
            )

        try {

            require(
                masterKey.size == 32,
            ) {
                "Master Key inválida."
            }

            ensureDeviceKey()

            val salt =
                ByteArray(
                    16,
                ).also {
                    SecureRandom()
                        .nextBytes(it)
                }

            val derived =
                derivePinKey(
                    pin,
                    salt,
                )

            try {

                val iv =
                    ByteArray(
                        12,
                    ).also {
                        SecureRandom()
                            .nextBytes(it)
                    }

                val cipher =
                    Cipher.getInstance(
                        AES_TRANSFORMATION,
                    )

                cipher.init(
                    Cipher.ENCRYPT_MODE,
                    SecretKeySpec(
                        derived,
                        "AES",
                    ),
                    GCMParameterSpec(
                        128,
                        iv,
                    ),
                )

                cipher.updateAAD(
                    DOMAIN,
                )

                cipher.updateAAD(
                    scope.toByteArray(
                        Charsets.UTF_8,
                    ),
                )

                val encrypted =
                    cipher.doFinal(
                        masterKey,
                    )

                val candidate =
                    JSONObject()
                        .put(
                            "version",
                            VERSION,
                        )
                        .put(
                            "salt",
                            encode(salt),
                        )
                        .put(
                            "iv",
                            encode(iv),
                        )
                        .put(
                            "data",
                            encode(encrypted),
                        )
                        .toString()

                check(
                    prefs.edit()
                        .putString(
                            CANDIDATE_KEY,
                            candidate,
                        )
                        .commit(),
                ) {
                    "Não foi possível salvar o novo PIN."
                }

                val verified =
                    decryptEnvelope(
                        candidate,
                        pin,
                    )

                try {

                    check(
                        MessageDigest.isEqual(
                            masterKey,
                            verified,
                        ),
                    ) {
                        "Não foi possível validar o novo PIN."
                    }

                } finally {
                    verified.fill(0)
                }

                check(
                    prefs.edit()
                        .putString(
                            ENVELOPE_KEY,
                            candidate,
                        )
                        .remove(
                            CANDIDATE_KEY,
                        )
                        .putInt(
                            FAILED_ATTEMPTS_KEY,
                            0,
                        )
                        .remove(
                            LOCKED_UNTIL_KEY,
                        )
                        .commit(),
                ) {
                    "Não foi possível concluir a configuração do PIN."
                }

            } catch (error: Exception) {

                /*
                 * Remove apenas o candidato.
                 * Um envelope anterior continua funcionando.
                 */
                runCatching {
                    prefs.edit()
                        .remove(
                            CANDIDATE_KEY,
                        )
                        .commit()
                }

                throw error

            } finally {
                derived.fill(0)
            }

        } finally {
            masterKey.fill(0)
        }
    }

    /**
     * Abre a Master Key usando o PIN local.
     */
    fun unlock(
        pin: String,
    ): ByteArray {

        validatePin(
            pin,
        )

        ensureNotLocked()

        check(
            keys.containsAlias(
                alias,
            ),
        ) {
            "A proteção segura deste dispositivo não está disponível."
        }

        val envelope =
            requireNotNull(
                prefs.getString(
                    ENVELOPE_KEY,
                    null,
                ),
            ) {
                "Nenhum PIN local foi configurado neste dispositivo."
            }

        return try {

            val masterKey =
                decryptEnvelope(
                    envelope,
                    pin,
                )

            resetFailedAttempts()

            masterKey

        } catch (
            error: AEADBadTagException
        ) {

            registerFailedAttempt()

            val status =
                status()

            if (
                status.lockedUntil != null
            ) {
                throw IllegalArgumentException(
                    "PIN incorreto. Muitas tentativas; PIN temporariamente bloqueado.",
                )
            }

            val remaining =
                maxOf(
                    0,
                    MAX_FAILED_ATTEMPTS -
                            status.failedAttempts,
                )

            throw IllegalArgumentException(
                "PIN incorreto. $remaining tentativa(s) restante(s).",
            )
        }
    }

    private fun decryptEnvelope(
        envelopeText: String,
        pin: String,
    ): ByteArray {

        validatePin(
            pin,
        )

        val envelope =
            JSONObject(
                envelopeText,
            )

        require(
            envelope.getInt(
                "version",
            ) == VERSION,
        ) {
            "Versão do PIN local não suportada."
        }

        val salt =
            decode(
                envelope.getString(
                    "salt",
                ),
            )

        val iv =
            decode(
                envelope.getString(
                    "iv",
                ),
            )

        val encrypted =
            decode(
                envelope.getString(
                    "data",
                ),
            )

        require(
            salt.size == 16,
        ) {
            "Salt do PIN inválido."
        }

        require(
            iv.size == 12,
        ) {
            "IV do PIN inválido."
        }

        val derived =
            derivePinKey(
                pin,
                salt,
            )

        return try {

            val cipher =
                Cipher.getInstance(
                    AES_TRANSFORMATION,
                )

            cipher.init(
                Cipher.DECRYPT_MODE,
                SecretKeySpec(
                    derived,
                    "AES",
                ),
                GCMParameterSpec(
                    128,
                    iv,
                ),
            )

            cipher.updateAAD(
                DOMAIN,
            )

            cipher.updateAAD(
                scope.toByteArray(
                    Charsets.UTF_8,
                ),
            )

            val plain =
                cipher.doFinal(
                    encrypted,
                )

            if (
                plain.size != 32
            ) {
                plain.fill(0)

                error(
                    "Master Key local inválida.",
                )
            }

            plain

        } finally {
            derived.fill(0)
        }
    }

    /**
     * Remove apenas o PIN LOCAL deste dispositivo.
     *
     * Não altera:
     * - PIN legado;
     * - Supabase;
     * - senha mestra;
     * - biometria;
     * - dados armazenados no Cofre.
     */
    fun clear() {

        if (
            keys.containsAlias(
                alias,
            )
        ) {
            keys.deleteEntry(
                alias,
            )
        }

        check(
            prefs.edit()
                .clear()
                .commit(),
        ) {
            "Não foi possível limpar o PIN local."
        }
    }
}