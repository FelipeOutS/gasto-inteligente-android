package com.gastointeligente.app

import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.webkit.WebView
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.webkit.JavaScriptReplyProxy
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import org.json.JSONObject

/**
 * Canal seguro entre o WebView e o Android nativo.
 *
 * Não utiliza addJavascriptInterface para expor tokens/chaves.
 * As mensagens são limitadas ao origin oficial e ao main frame.
 */
internal class WebSecureSession(
    private val activity: FragmentActivity,
    private val web: WebView,
) {

    private val store = SecureSessionStore(activity)

    private val origins = setOf(
        "https://gastointeligente.com.br",
        "https://www.gastointeligente.com.br",
    )

    private val handler = Handler(Looper.getMainLooper())

    private var generation = 0
    private var closed = false

    private var prompt: BiometricPrompt? = null
    private var deviceCredentialPrompt = false

    fun onHostStopped() {
        // The system credential Activity legitimately stops our Activity.
        // Only this short-lived, cryptographically bound prompt may continue.
        if (!deviceCredentialPrompt) cancel()
    }
    private var timeout: Runnable? = null

    private var pendingVaultEnrollment: SecureSessionStore? = null

    fun attach(): Boolean {

        if (
            !WebViewFeature.isFeatureSupported(
                WebViewFeature.WEB_MESSAGE_LISTENER,
            ) ||
            !WebViewFeature.isFeatureSupported(
                WebViewFeature.DOCUMENT_START_SCRIPT,
            )
        ) {
            return false
        }

        WebViewCompat.addWebMessageListener(
            web,
            "GastoSecureSession",
            origins,
        ) { _, message, origin, mainFrame, reply ->

            if (
                closed ||
                !mainFrame ||
                origin.toString().trimEnd('/') !in origins
            ) {
                return@addWebMessageListener
            }

            val raw =
                message.data
                    ?: return@addWebMessageListener

            if (raw.length > 26000) {
                return@addWebMessageListener
            }

            val data =
                try {
                    JSONObject(raw)
                } catch (_: Exception) {
                    return@addWebMessageListener
                }

            val id =
                data.optString("id")

            if (id.length !in 1..32) {
                return@addWebMessageListener
            }

            try {

                when (
                    data.optString("type")
                ) {

                    "status" -> {
                        respond(
                            reply,
                            id,
                            true,
                        )
                    }

                    "clear" -> {
                        cancel()
                        store.clear()

                        respond(
                            reply,
                            id,
                            true,
                        )
                    }

                    "save" -> {

                        check(
                            prompt == null,
                        )

                        check(
                            BiometricManager
                                .from(activity)
                                .canAuthenticate(
                                    store.allowedAuthenticators(forSave = true),
                                ) ==
                                    BiometricManager.BIOMETRIC_SUCCESS,
                        )

                        store.save(
                            JSONObject()
                                .put(
                                    "access_token",
                                    data.getString(
                                        "access_token",
                                    ),
                                )
                                .put(
                                    "refresh_token",
                                    data.getString(
                                        "refresh_token",
                                    ),
                                )
                                .put(
                                    "userId",
                                    data.getString(
                                        "userId",
                                    ),
                                )
                                .put(
                                    "email",
                                    data.getString(
                                        "email",
                                    ),
                                ),
                        )

                        respond(
                            reply,
                            id,
                            true,
                        )
                    }

                    "unlock" -> {
                        unlock(
                            reply,
                            id,
                        )
                    }

                    /*
                     * =====================================================
                     * COFRE — BIOMETRIA NATIVA
                     * =====================================================
                     */

                    "vaultStatus" -> {

                        reply.postMessage(
                            JSONObject()
                                .put(
                                    "id",
                                    id,
                                )
                                .put(
                                    "success",
                                    true,
                                )
                                .put(
                                    "available",
                                    BiometricManager
                                        .from(activity)
                                        .canAuthenticate(
                                            BIOMETRIC_STRONG,
                                        ) ==
                                            BiometricManager.BIOMETRIC_SUCCESS,
                                )
                                .toString(),
                        )
                    }

                    "vaultSave",
                    "vaultUnlock",
                    "vaultClear" -> {

                        val userId =
                            data.getString(
                                "userId",
                            )

                        val vault =
                            SecureSessionStore(
                                activity,
                                userId,
                            )

                        when (
                            data.getString(
                                "type",
                            )
                        ) {

                            "vaultClear" -> {

                                cancel()

                                vault.clear()

                                respondVault(
                                    reply = reply,
                                    id = id,
                                    userId = userId,
                                    payload = null,
                                    cleared = true,
                                )
                            }

                            "vaultSave" -> {

                                check(
                                    prompt == null,
                                )

                                check(
                                    BiometricManager
                                        .from(activity)
                                        .canAuthenticate(
                                            BIOMETRIC_STRONG,
                                        ) ==
                                            BiometricManager.BIOMETRIC_SUCCESS,
                                )

                                /*
                                 * A Master Key chega somente em memória.
                                 */
                                val payload =
                                    JSONObject()
                                        .put(
                                            "userId",
                                            userId,
                                        )
                                        .put(
                                            "master_key",
                                            data.getString(
                                                "master_key",
                                            ),
                                        )

                                vault.validatePayload(
                                    payload,
                                )

                                /*
                                 * Enrollment novo substitui somente
                                 * o quick unlock biométrico local.
                                 */
                                vault.clear()

                                vault.save(
                                    payload,
                                )

                                pendingVaultEnrollment =
                                    vault

                                unlock(
                                    reply = reply,
                                    id = id,
                                    targetStore = vault,
                                    vaultUserId = userId,
                                )
                            }

                            else -> {

                                unlock(
                                    reply = reply,
                                    id = id,
                                    targetStore = vault,
                                    vaultUserId = userId,
                                )
                            }
                        }
                    }

                    /*
                     * =====================================================
                     * COFRE — NOVO PIN LOCAL SEGURO
                     * =====================================================
                     *
                     * IMPORTANTE:
                     * Isto NÃO remove nem modifica o PIN legado do servidor.
                     */

                    "vaultPinStatus" -> {

                        val userId =
                            data.getString(
                                "userId",
                            )

                        try {

                            val pinStore =
                                VaultPinStore(
                                    activity,
                                    userId,
                                )

                            respondVaultPin(
                                reply = reply,
                                id = id,
                                userId = userId,
                                success = true,
                                status = pinStore.status(),
                            )

                        } catch (error: Exception) {

                            respondVaultPin(
                                reply = reply,
                                id = id,
                                userId = userId,
                                success = false,
                                error = safePinError(
                                    error,
                                ),
                            )
                        }
                    }

                    "vaultPinEnroll" -> {

                        val userId =
                            data.getString(
                                "userId",
                            )

                        try {

                            val pinStore =
                                VaultPinStore(
                                    activity,
                                    userId,
                                )

                            pinStore.enroll(
                                pin =
                                    data.getString(
                                        "pin",
                                    ),
                                masterKeyBase64 =
                                    data.getString(
                                        "master_key",
                                    ),
                            )

                            respondVaultPin(
                                reply = reply,
                                id = id,
                                userId = userId,
                                success = true,
                                status = pinStore.status(),
                            )

                        } catch (error: Exception) {

                            respondVaultPin(
                                reply = reply,
                                id = id,
                                userId = userId,
                                success = false,
                                error = safePinError(
                                    error,
                                ),
                            )
                        }
                    }

                    "vaultPinUnlock" -> {

                        val userId =
                            data.getString(
                                "userId",
                            )

                        try {

                            val pinStore =
                                VaultPinStore(
                                    activity,
                                    userId,
                                )

                            val masterKey =
                                pinStore.unlock(
                                    data.getString(
                                        "pin",
                                    ),
                                )

                            try {

                                val masterKeyBase64 =
                                    Base64.encodeToString(
                                        masterKey,
                                        Base64.NO_WRAP,
                                    )

                                respondVaultPin(
                                    reply = reply,
                                    id = id,
                                    userId = userId,
                                    success = true,
                                    masterKey = masterKeyBase64,
                                    status = pinStore.status(),
                                )

                            } finally {

                                /*
                                 * Limpa o ByteArray nativo imediatamente
                                 * depois de construir a resposta.
                                 */
                                masterKey.fill(0)
                            }

                        } catch (error: Exception) {

                            val status =
                                runCatching {
                                    VaultPinStore(
                                        activity,
                                        userId,
                                    ).status()
                                }.getOrNull()

                            respondVaultPin(
                                reply = reply,
                                id = id,
                                userId = userId,
                                success = false,
                                status = status,
                                error = safePinError(
                                    error,
                                ),
                            )
                        }
                    }

                    "vaultPinClear" -> {

                        val userId =
                            data.getString(
                                "userId",
                            )

                        try {

                            val pinStore =
                                VaultPinStore(
                                    activity,
                                    userId,
                                )

                            pinStore.clear()

                            respondVaultPin(
                                reply = reply,
                                id = id,
                                userId = userId,
                                success = true,
                                status = pinStore.status(),
                            )

                        } catch (error: Exception) {

                            respondVaultPin(
                                reply = reply,
                                id = id,
                                userId = userId,
                                success = false,
                                error = safePinError(
                                    error,
                                ),
                            )
                        }
                    }

                    else -> {

                        respond(
                            reply,
                            id,
                            false,
                        )
                    }
                }

            } catch (_: Exception) {

                respond(
                    reply,
                    id,
                    false,
                )
            }
        }

        WebViewCompat.addDocumentStartJavaScript(
            web,
            activity.assets
                .open(
                    "secure-session.js",
                )
                .bufferedReader()
                .use {
                    it.readText()
                },
            origins,
        )

        return true
    }

    private fun respond(
        reply: JavaScriptReplyProxy,
        id: String,
        success: Boolean,
        tokens: JSONObject? = null,
    ) {

        if (closed) {
            return
        }

        val response =
            JSONObject()
                .put(
                    "id",
                    id,
                )
                .put(
                    "success",
                    success,
                )
                .put(
                    "saved",
                    runCatching {
                        store.hasSession()
                    }.getOrDefault(
                        false,
                    ),
                )

        if (
            success &&
            tokens != null
        ) {

            response
                .put(
                    "user_id",
                    tokens.getString(
                        "userId",
                    ),
                )
                .put(
                    "email",
                    tokens.getString(
                        "email",
                    ),
                )

            if (
                web.url != OfflinePage.URL
            ) {

                response
                    .put(
                        "access_token",
                        tokens.getString(
                            "access_token",
                        ),
                    )
                    .put(
                        "refresh_token",
                        tokens.getString(
                            "refresh_token",
                        ),
                    )
            }

        } else if (!success) {

            response.put(
                "error",
                "Não foi possível usar a sessão segura. Entre com sua senha ou tente novamente.",
            )
        }

        reply.postMessage(
            response.toString(),
        )
    }

    private fun respondVault(
        reply: JavaScriptReplyProxy,
        id: String,
        userId: String,
        payload: JSONObject?,
        cleared: Boolean = false,
    ) {

        if (closed) {
            return
        }

        val valid =
            payload?.optString(
                "userId",
            ) == userId

        val response =
            JSONObject()
                .put(
                    "id",
                    id,
                )
                .put(
                    "success",
                    valid || cleared,
                )
                .put(
                    "userId",
                    userId,
                )

        if (valid) {

            response.put(
                "master_key",
                payload!!.getString(
                    "master_key",
                ),
            )

        } else if (!cleared) {

            response.put(
                "error",
                "Não foi possível abrir o Cofre com biometria. Use a senha mestra ou o PIN do Cofre.",
            )
        }

        reply.postMessage(
            response.toString(),
        )
    }

    /**
     * Resposta exclusiva do novo PIN local.
     */
    private fun respondVaultPin(
        reply: JavaScriptReplyProxy,
        id: String,
        userId: String,
        success: Boolean,
        masterKey: String? = null,
        status: VaultPinStore.Status? = null,
        error: String? = null,
    ) {

        if (closed) {
            return
        }

        val response =
            JSONObject()
                .put(
                    "id",
                    id,
                )
                .put(
                    "success",
                    success,
                )
                .put(
                    "userId",
                    userId,
                )

        if (status != null) {

            response
                .put(
                    "configured",
                    status.configured,
                )
                .put(
                    "failed_attempts",
                    status.failedAttempts,
                )

            if (
                status.lockedUntil != null
            ) {
                response.put(
                    "locked_until",
                    status.lockedUntil,
                )
            } else {
                response.put(
                    "locked_until",
                    JSONObject.NULL,
                )
            }
        }

        if (
            success &&
            masterKey != null
        ) {

            response.put(
                "master_key",
                masterKey,
            )
        }

        if (!success) {

            response.put(
                "error",
                error
                    ?: "Não foi possível usar o PIN seguro do Cofre.",
            )
        }

        reply.postMessage(
            response.toString(),
        )
    }

    /**
     * Nunca devolve detalhes criptográficos internos ao WebView.
     */
    private fun safePinError(
        error: Exception,
    ): String {

        val message =
            error.message.orEmpty()

        return when {

            message.startsWith(
                "PIN incorreto",
            ) -> message

            message.startsWith(
                "Muitas tentativas",
            ) -> message

            message.startsWith(
                "O PIN deve",
            ) -> message

            message.contains(
                "Nenhum PIN local",
            ) ->
                "Nenhum PIN seguro foi configurado neste dispositivo."

            else ->
                "Não foi possível usar o PIN seguro do Cofre."
        }
    }

    private fun unlock(
        reply: JavaScriptReplyProxy,
        id: String,
        targetStore: SecureSessionStore = store,
        vaultUserId: String? = null,
    ) {

        if (prompt != null) {

            respond(
                reply,
                id,
                false,
            )

            return
        }

        val current =
            generation

        val locked =
            try {

                targetStore.prepareUnlock()

            } catch (error: Exception) {
                if (SessionFailurePolicy.invalidatedKey(error)) runCatching { targetStore.clear() }

                pendingVaultEnrollment =
                    null

                respond(
                    reply,
                    id,
                    false,
                )

                return
            }

        var completed =
            false

        fun finish(
            tokens: JSONObject? = null,
        ) {

            if (
                completed ||
                closed ||
                generation != current
            ) {
                return
            }

            completed =
                true
            deviceCredentialPrompt = false

            timeout?.let {
                handler.removeCallbacks(
                    it,
                )
            }

            timeout =
                null

            prompt =
                null

            if (
                tokens == null
            ) {

                pendingVaultEnrollment
                    ?.let {
                        runCatching {
                            it.clear()
                        }
                    }
            }

            pendingVaultEnrollment =
                null

            if (
                vaultUserId == null
            ) {

                respond(
                    reply,
                    id,
                    tokens != null,
                    tokens,
                )

            } else {

                respondVault(
                    reply,
                    id,
                    vaultUserId,
                    tokens,
                )
            }
        }

        val biometric =
            BiometricPrompt(
                activity,
                ContextCompat.getMainExecutor(
                    activity,
                ),
                object :
                    BiometricPrompt.AuthenticationCallback() {

                    override fun onAuthenticationSucceeded(
                        result:
                        BiometricPrompt.AuthenticationResult,
                    ) {

                        if (
                            completed ||
                            closed ||
                            generation != current
                        ) {
                            return
                        }

                        if (BuildConfig.DEBUG) android.util.Log.d("GIAuthDiagnostic", "success cipher=" + (result.cryptoObject?.cipher != null))
                        try {

                            finish(
                                targetStore.unlock(
                                    locked,
                                    requireNotNull(
                                        result.cryptoObject
                                            ?.cipher,
                                    ),
                                ),
                            )

                        } catch (error: Exception) {
                            if (BuildConfig.DEBUG) android.util.Log.d("GIAuthDiagnostic", "decrypt_error=" + error.javaClass.simpleName)

                            if (SessionFailurePolicy.invalidatedKey(error)) runCatching { targetStore.clear() }
                            finish()
                        }
                    }

                    override fun onAuthenticationError(
                        errorCode: Int,
                        errString: CharSequence,
                    ) {

                        if (BuildConfig.DEBUG) android.util.Log.d("GIAuthDiagnostic", "authentication_error=" + errorCode)
                        finish()
                    }
                },
            )

        prompt =
            biometric

        timeout =
            Runnable {

                if (BuildConfig.DEBUG) android.util.Log.d("GIAuthDiagnostic", "timeout")
                biometric.cancelAuthentication()

                finish()

            }.also {

                handler.postDelayed(
                    it,
                    45000,
                )
            }

        try {

            deviceCredentialPrompt = vaultUserId == null &&
                targetStore.allowedAuthenticators() and BiometricManager.Authenticators.DEVICE_CREDENTIAL != 0
            biometric.authenticate(
                BiometricPrompt.PromptInfo
                    .Builder()
                    .setTitle(
                        if (
                            vaultUserId == null
                        ) {
                            "Entrar no Gasto Inteligente"
                        } else {
                            "Desbloquear Cofre Pessoal"
                        },
                    )
                    .setSubtitle(
                        if (
                            vaultUserId == null
                        ) {
                            "Confirme a biometria ou o bloqueio do celular para recuperar a sessão"
                        } else {
                            "Confirme a biometria para proteger ou abrir seu Cofre"
                        },
                    )
                    .setAllowedAuthenticators(targetStore.allowedAuthenticators())
                    .apply {
                        if (targetStore.allowedAuthenticators() and BiometricManager.Authenticators.DEVICE_CREDENTIAL == 0) {
                            setNegativeButtonText("Usar senha")
                        }
                    }
                    .build(),
                BiometricPrompt.CryptoObject(
                    locked.cipher,
                ),
            )

        } catch (_: Exception) {

            finish()
        }
    }

    fun cancel() {
        deviceCredentialPrompt = false

        generation++

        timeout?.let {
            handler.removeCallbacks(
                it,
            )
        }

        timeout =
            null

        pendingVaultEnrollment
            ?.let {
                runCatching {
                    it.clear()
                }
            }

        pendingVaultEnrollment =
            null

        val old =
            prompt

        prompt =
            null

        old?.cancelAuthentication()
    }

    fun close() {

        closed =
            true

        cancel()
    }
}