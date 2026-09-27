package com.gastointeligente.app

import android.content.Context
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse

/** Bundled, narrowly scoped fallback. Does not cache the production site's HTML/chunks. */
internal object OfflinePage {
    const val URL = "https://gastointeligente.com.br/__android/offline"
    fun intercept(context: Context, request: WebResourceRequest): WebResourceResponse? {
        if (request.method != "GET") return null
        val file = when (request.url.toString()) {
            URL -> "offline.html" to "text/html"
            "$URL.js" -> "offline.js" to "text/javascript"
            else -> return null
        }
        return WebResourceResponse(file.second, "UTF-8", 200, "OK", mapOf("Cache-Control" to "no-store"), context.assets.open(file.first))
    }
}
