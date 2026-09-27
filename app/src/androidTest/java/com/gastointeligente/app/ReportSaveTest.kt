package com.gastointeligente.app

import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import android.net.Uri
import android.util.Base64
import android.view.View
import android.view.ViewGroup
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intending
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.VerificationModes.times
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.espresso.intent.matcher.IntentMatchers.hasExtra
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.hamcrest.Matchers.allOf
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayInputStream
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class ReportSaveTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private fun findWeb(view: View): WebView? {
        if (view is WebView) return view
        if (view is ViewGroup) for (index in 0 until view.childCount) findWeb(view.getChildAt(index))?.let { return it }
        return null
    }
    private fun fixture(scenario: ActivityScenario<MainActivity>): WebView {
        val loaded = CountDownLatch(1)
        lateinit var web: WebView
        scenario.onActivity { activity ->
            web = requireNotNull(findWeb(activity.findViewById(android.R.id.content)))
            web.stopLoading()
            web.webViewClient = object : WebViewClient() {
                override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?) =
                    WebResourceResponse("text/html", "UTF-8", ByteArrayInputStream("<html>Salvar relatório local</html>".toByteArray()))
                override fun onPageFinished(view: WebView?, url: String?) {
                    if (url == "https://gastointeligente.com.br/android-save-fixture") loaded.countDown()
                }
            }
            web.loadUrl("https://gastointeligente.com.br/android-save-fixture")
        }
        assertTrue(loaded.await(20, TimeUnit.SECONDS))
        return web
    }
    private fun export(scenario: ActivityScenario<MainActivity>, web: WebView, bytes: ByteArray) {
        val encoded = JSONObject.quote(Base64.encodeToString(bytes, Base64.NO_WRAP))
        scenario.onActivity {
            web.evaluateJavascript("""(() => {const bytes = Uint8Array.from(atob($encoded), c => c.charCodeAt(0));
              const a = document.createElement('a'); const url = URL.createObjectURL(new Blob([bytes], {type:'text/csv'}));
              a.href = url; a.download = 'relatorio.csv'; a.click(); URL.revokeObjectURL(url);})()""", null)
        }
    }

    @Test fun selectedDocumentReceivesExactReportBytes() {
        val bytes = instrumentation.context.assets.open("reports/relatorio.csv").use { it.readBytes() }
        val destination = Uri.parse("content://com.gastointeligente.test.reports/report")
        Intents.init()
        try {
            intending(hasAction(Intent.ACTION_CREATE_DOCUMENT)).respondWith(Instrumentation.ActivityResult(Activity.RESULT_OK, Intent().setData(destination)))
            ActivityScenario.launch(MainActivity::class.java).use { scenario ->
                export(scenario, fixture(scenario), bytes)
                var saved = false
                for (attempt in 0 until 150) {
                    saved = try { instrumentation.targetContext.contentResolver.openInputStream(destination)?.use { it.readBytes().contentEquals(bytes) } == true } catch (_: Exception) { false }
                    if (saved) break
                    android.os.SystemClock.sleep(100)
                }
                assertTrue("Selected document did not receive the complete report", saved)
                intended(allOf(hasAction(Intent.ACTION_CREATE_DOCUMENT), hasExtra(Intent.EXTRA_TITLE, "relatorio.csv")))
            }
        } finally { Intents.release() }
    }

    @Test fun cancelingSaveAllowsAnotherExportAndRemovesTemporaryFiles() {
        Intents.init()
        try {
            intending(hasAction(Intent.ACTION_CREATE_DOCUMENT)).respondWith(Instrumentation.ActivityResult(Activity.RESULT_CANCELED, null))
            ActivityScenario.launch(MainActivity::class.java).use { scenario ->
                val web = fixture(scenario)
                for (number in 1..2) {
                    export(scenario, web, "teste;valor\nCafé;12,34".toByteArray())
                    var completed = false
                    for (attempt in 0 until 150) {
                        completed = Intents.getIntents().count { it.action == Intent.ACTION_CREATE_DOCUMENT } == number &&
                            File(instrumentation.targetContext.cacheDir, "reports").listFiles().orEmpty().isEmpty()
                        if (completed) break
                        android.os.SystemClock.sleep(100)
                    }
                    assertTrue("Canceled export was not released", completed)
                    instrumentation.waitForIdleSync()
                }
                intended(hasAction(Intent.ACTION_CREATE_DOCUMENT), times(2))
            }
        } finally { Intents.release() }
    }
}
