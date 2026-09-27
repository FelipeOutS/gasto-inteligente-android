package com.gastointeligente.app

import java.net.URI
import java.util.Locale

internal enum class NavigationTarget { INTERNAL, EXTERNAL, BLOCKED }

internal fun navigationTarget(url: String?, mainFrame: Boolean = true, userGesture: Boolean = true, debug: Boolean = BuildConfig.DEBUG): NavigationTarget {
    val uri = try { URI(url ?: return NavigationTarget.BLOCKED) } catch (_: Exception) {
        return NavigationTarget.BLOCKED
    }
    val scheme = uri.scheme?.lowercase(Locale.ROOT) ?: return NavigationTarget.BLOCKED
    val host = uri.host?.lowercase(Locale.ROOT)
    val trustedHost = host == "gastointeligente.com.br" || host?.endsWith(".gastointeligente.com.br") == true
    if (uri.userInfo != null) return NavigationTarget.BLOCKED
    if (debug && scheme == "http" && host == "127.0.0.1" && uri.port == 5173) return NavigationTarget.INTERNAL
    if (trustedHost && scheme == "https" && (uri.port == -1 || uri.port == 443)) return NavigationTarget.INTERNAL
    if (!mainFrame) return NavigationTarget.BLOCKED
    if (scheme in setOf("https", "http") && !host.isNullOrBlank() && !trustedHost) return NavigationTarget.EXTERNAL
    if (userGesture && scheme in setOf("mailto", "tel", "whatsapp") && !uri.schemeSpecificPart.isNullOrBlank()) return NavigationTarget.EXTERNAL
    return NavigationTarget.BLOCKED
}
