package com.gastointeligente.app

import java.net.URI
import java.net.URLDecoder
import java.net.URLEncoder
import java.security.SecureRandom

internal data class OAuthAttempt(val state: String, val startedAt: Long)
internal data class OAuthStart(val attempt: OAuthAttempt, val browserUrl: String)
internal data class OAuthReturn(val pageUrl: String?, val error: String?)

/** Matches the existing Lovable broker. No alternate Google client or Supabase provider is introduced. */
internal object AndroidOAuth {
    const val ORIGIN = "https://gastointeligente.com.br"
    const val CALLBACK_PATH = "/auth/android/callback"
    const val MAX_AGE_MS = 10 * 60 * 1000L
    private fun encode(value: String) = URLEncoder.encode(value, "UTF-8")
    private fun parameters(value: String?): Map<String, String> {
        if (value.isNullOrEmpty()) return emptyMap()
        val pairs = value.split('&').map {
            val parts = it.split('=', limit = 2)
            URLDecoder.decode(parts[0], "UTF-8") to URLDecoder.decode(parts.getOrElse(1) { "" }, "UTF-8")
        }
        require(pairs.map { it.first }.distinct().size == pairs.size)
        return pairs.toMap()
    }
    private fun trusted(uri: URI) = uri.scheme == "https" &&
        uri.host == "gastointeligente.com.br" && uri.userInfo == null && (uri.port == -1 || uri.port == 443)

    fun start(url: String, now: Long): OAuthStart? = try {
        val uri = URI(url)
        val params = parameters(uri.rawQuery)
        if (!trusted(uri) || uri.path != "/~oauth/initiate" || params["provider"] != "google") null
        else {
            val bytes = ByteArray(32).also { SecureRandom().nextBytes(it) }
            val state = bytes.joinToString("") { "%02x".format(it.toInt() and 255) }
            val callback = "$ORIGIN$CALLBACK_PATH?request=$state"
            OAuthStart(OAuthAttempt(state, now), "$ORIGIN/~oauth/initiate?provider=google&redirect_uri=${encode(callback)}&state=$state")
        }
    } catch (_: Exception) { null }

    fun isCallback(url: String): Boolean = try {
        val uri = URI(url)
        trusted(uri) && uri.path == CALLBACK_PATH
    } catch (_: Exception) { false }

    fun accept(url: String, attempt: OAuthAttempt?, now: Long): OAuthReturn? = try {
        require(url.length <= 24000 && attempt != null)
        require(now - attempt.startedAt in 0..MAX_AGE_MS)
        val uri = URI(url)
        require(trusted(uri) && uri.path == CALLBACK_PATH)
        require(parameters(uri.rawQuery)["request"] == attempt.state)
        val data = parameters(uri.rawFragment)
        require(data["state"] == null || data["state"] == attempt.state)
        if (data.containsKey("error")) OAuthReturn(null, "O login não foi concluído. Tente novamente.")
        else {
            val access = requireNotNull(data["access_token"])
            val refresh = requireNotNull(data["refresh_token"])
            require(access.length in 1..16000 && refresh.length in 1..4096)
            val expires = requireNotNull(data["expires_in"]?.toLongOrNull())
            require(expires in 1..86400)
            require(data["token_type"].equals("bearer", ignoreCase = true))
            OAuthReturn("$ORIGIN$CALLBACK_PATH#access_token=${encode(access)}&refresh_token=${encode(refresh)}&expires_in=$expires&token_type=bearer", null)
        }
    } catch (_: Exception) { null }
}
