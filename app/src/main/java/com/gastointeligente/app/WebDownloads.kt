package com.gastointeligente.app

import android.util.Base64
import android.webkit.WebView
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import org.json.JSONObject
import java.io.File
import java.util.concurrent.Executors

internal class WebDownloads(private val web: WebView, private val canStart: () -> Boolean, private val onReady: (ReadyDownload) -> Boolean) {
    private val worker = Executors.newSingleThreadExecutor()
    private val assembler = DownloadAssembler(File(web.context.cacheDir, "reports"))
    @Volatile private var closed = false
    @Volatile private var generation = 0
    private val origins = setOf("https://gastointeligente.com.br", "https://www.gastointeligente.com.br")

    fun attach(): Boolean {
        if (!WebViewFeature.isFeatureSupported(WebViewFeature.WEB_MESSAGE_LISTENER) ||
            !WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT)) return false
        WebViewCompat.addWebMessageListener(web, "GastoDownloads", origins) { _, message, origin, mainFrame, reply ->
            if (closed || !mainFrame || origin.toString().trimEnd('/') !in origins) return@addWebMessageListener
            val raw = message.data ?: return@addWebMessageListener
            if (raw.length > 46000) return@addWebMessageListener
            val header = try { JSONObject(raw) } catch (_: Exception) { return@addWebMessageListener }
            if (header.optString("type") == "begin" && !canStart()) {
                reply.postMessage(JSONObject().put("id", header.optString("id")).put("ok", false)
                    .put("error", "Conclua o salvamento anterior antes de exportar outro arquivo.").toString())
                return@addWebMessageListener
            }
            val currentGeneration = generation
            worker.execute {
                if (closed || currentGeneration != generation) return@execute
                var id = ""
                try {
                    val data = JSONObject(raw)
                    id = data.getString("id")
                    when (data.getString("type")) {
                        "begin" -> assembler.begin(id, data.getString("name"), data.getString("mime"), data.getLong("size"))
                        "chunk" -> assembler.append(id, Base64.decode(data.getString("data"), Base64.NO_WRAP))
                        "end" -> {
                            val ready = assembler.finish(id)
                            web.post {
                                if (closed || currentGeneration != generation || !onReady(ready)) ready.file.delete()
                            }
                        }
                        "cancel" -> assembler.cancel()
                        else -> error("Invalid download message")
                    }
                    val response = JSONObject().put("id", id).put("ok", true).toString()
                    web.post { if (!closed) reply.postMessage(response) }
                } catch (_: Exception) {
                    assembler.cancel()
                    val response = JSONObject().put("id", id).put("ok", false)
                        .put("error", "Não foi possível receber o relatório. Tente novamente.").toString()
                    web.post { if (!closed) reply.postMessage(response) }
                }
            }
        }
        val script = web.context.assets.open("report-downloads.js").bufferedReader().use { it.readText() }
        WebViewCompat.addDocumentStartJavaScript(web, script, origins)
        return true
    }

    fun onNavigation() {
        if (closed) return
        generation++
        worker.execute { assembler.cancel() }
    }

    fun close() {
        if (closed) return
        closed = true
        worker.execute { assembler.cancel() }
        worker.shutdown()
    }
}
