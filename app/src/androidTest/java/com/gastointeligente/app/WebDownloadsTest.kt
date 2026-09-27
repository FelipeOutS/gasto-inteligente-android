package com.gastointeligente.app

import android.util.Base64
import android.view.ViewGroup
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayInputStream
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class WebDownloadsTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()

    private fun transfer(extension: String, mime: String, dispatch: Boolean = false) {
        val expected = instrumentation.context.assets.open("reports/relatorio.$extension").use { it.readBytes() }
        val loaded = CountDownLatch(1)
        val received = CountDownLatch(1)
        var report: ReadyDownload? = null
        lateinit var web: WebView
        lateinit var downloads: WebDownloads
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            try {
                scenario.onActivity { activity ->
                    web = WebView(activity)
                    web.settings.javaScriptEnabled = true
                    downloads = WebDownloads(web, { true }) { report = it; received.countDown(); true }
                    assertTrue("Update the test WebView to support document-start messaging", downloads.attach())
                    web.webViewClient = object : WebViewClient() {
                        override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?) =
                            WebResourceResponse("text/html", "UTF-8", ByteArrayInputStream("<html><body>Exportação local</body></html>".toByteArray()))
                        override fun onPageFinished(view: WebView?, url: String?) { loaded.countDown() }
                    }
                    activity.setContentView(web)
                    web.loadUrl("https://gastointeligente.com.br/android-download-fixture")
                }
                assertTrue(loaded.await(20, TimeUnit.SECONDS))
                scenario.onActivity {
                    val content = JSONObject.quote(Base64.encodeToString(expected, Base64.NO_WRAP))
                    val click = if (dispatch) "a.dispatchEvent(new MouseEvent('click'))" else "a.click()"
                    web.evaluateJavascript("""(() => {
                        const bytes = Uint8Array.from(atob($content), c => c.charCodeAt(0));
                        const url = URL.createObjectURL(new Blob([bytes], {type: '$mime'}));
                        const a = document.createElement('a'); a.href = url; a.download = 'relatorio.$extension';
                        $click; URL.revokeObjectURL(url);
                    })()""", null)
                }
                assertTrue("Report did not reach Android", received.await(30, TimeUnit.SECONDS))
                val result = requireNotNull(report)
                assertArrayEquals("Exported report was modified in transit", expected, result.file.readBytes())
                assertEquals("relatorio.$extension", result.name)
                assertEquals(mime.substringBefore(';'), result.mime)
            } finally {
                scenario.onActivity {
                    downloads.close()
                    (web.parent as? ViewGroup)?.removeView(web)
                    web.destroy()
                }
                report?.file?.delete()
            }
        }
    }

    @Test fun csvFromSiteSurvivesImmediateUrlRevocation() = transfer("csv", "text/csv;charset=utf-8")
    @Test fun excelFromSitePreservesBinaryContentAcrossChunks() = transfer("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    @Test fun pdfFromSiteSupportsJsPdfDetachedSyntheticClick() = transfer("pdf", "application/pdf", dispatch = true)

    @Test fun externalPagesDoNotReceiveDownloadBridge() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            val loaded = CountDownLatch(1)
            val evaluated = CountDownLatch(1)
            lateinit var web: WebView
            lateinit var downloads: WebDownloads
            var result = ""
            scenario.onActivity { activity ->
                web = WebView(activity)
                web.settings.javaScriptEnabled = true
                downloads = WebDownloads(web, { true }) { fail("Untrusted origin exported a report"); false }
                assertTrue(downloads.attach())
                web.webViewClient = object : WebViewClient() {
                    override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?) =
                        WebResourceResponse("text/html", "UTF-8", ByteArrayInputStream("<html>External fixture</html>".toByteArray()))
                    override fun onPageFinished(view: WebView?, url: String?) { loaded.countDown() }
                }
                activity.setContentView(web)
                web.loadUrl("https://example.org/")
            }
            assertTrue(loaded.await(20, TimeUnit.SECONDS))
            scenario.onActivity { web.evaluateJavascript("typeof window.GastoDownloads") { result = it; evaluated.countDown() } }
            assertTrue(evaluated.await(5, TimeUnit.SECONDS))
            assertEquals("\"undefined\"", result)
            scenario.onActivity { downloads.close(); (web.parent as? ViewGroup)?.removeView(web); web.destroy() }
        }
    }
}
