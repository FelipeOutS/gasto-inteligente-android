package com.gastointeligente.app

import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayInputStream
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/** Actual bundled form, queues and synchronizers; synthetic identity and remote writer.
 * Both pages share the production origin ONLY inside the isolated test package.
 * Every request is intercepted; no request is forwarded to production.
 */
@RunWith(AndroidJUnit4::class)
class OfflineReturnOnlineTest {
    private val origin = "https://gastointeligente.com.br"
    private fun eval(scenario: ActivityScenario<MainActivity>, web: WebView, script: String): String {
        val latch = CountDownLatch(1)
        var result = ""
        scenario.onActivity { web.evaluateJavascript(script) { result = it; latch.countDown() } }
        assertTrue(latch.await(5, TimeUnit.SECONDS))
        return result
    }
    private fun waitFor(scenario: ActivityScenario<MainActivity>, web: WebView, script: String, expected: String) {
        val deadline = android.os.SystemClock.elapsedRealtime() + 15000
        var actual = ""
        while (android.os.SystemClock.elapsedRealtime() < deadline) {
            actual = eval(scenario, web, script)
            if (actual == expected) return
            android.os.SystemClock.sleep(100)
        }
        assertEquals(script, expected, actual)
    }
    private fun load(scenario: ActivityScenario<MainActivity>, path: String): WebView {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val assets = instrumentation.targetContext.assets
        val fixture = instrumentation.context.assets.open("offline-accounts-fixture.js").use { it.readBytes() }
        lateinit var web: WebView
        scenario.onActivity { activity ->
            web = WebView(activity)
            web.settings.javaScriptEnabled = true
            web.settings.domStorageEnabled = true
            web.settings.blockNetworkLoads = true
            web.setNetworkAvailable(false)
            web.webViewClient = object : WebViewClient() {
                override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse {
                    val url = request?.url.toString()
                    val resource = when (url) {
                        "$origin/__android/offline" -> "text/html" to assets.open("offline.html").use { it.readBytes() }
                        "$origin/__android/offline.js" -> "text/javascript" to assets.open("offline.js").use { it.readBytes() }
                        "$origin/app" -> "text/html" to "<html><script src='/test-sync.js'></script></html>".toByteArray()
                        "$origin/test-sync.js" -> "text/javascript" to fixture
                        else -> "text/plain" to ByteArray(0)
                    }
                    return WebResourceResponse(resource.first, "UTF-8", ByteArrayInputStream(resource.second))
                }
            }
            activity.setContentView(web)
            web.loadUrl(origin + path)
        }
        return web
    }
    @Test fun offlineFormReturnsOnlineAfterRestartAndRetriesWithoutDuplicates() {
        assertTrue(InstrumentationRegistry.getInstrumentation().targetContext.packageName.endsWith(".offlinevalidation"))
        val owner = "return-online-${System.currentTimeMillis()}"
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            val web = load(scenario, "/__android/offline")
            waitFor(scenario, web, "typeof document.getElementById('expense')?.onsubmit", "\"function\"")
            // Native credential authentication has its own manual CryptoObject test.
            eval(scenario, web, "window.dispatchEvent(new CustomEvent('AndroidSecureSessionResult',{detail:{success:true,user_id:'$owner'}}))")
            waitFor(scenario, web, "document.getElementById('content').hidden", "false")
            eval(scenario, web, "document.getElementById('description').value='Synthetic return expense';document.getElementById('amount').value='85,00';document.getElementById('date').value='2026-08-12';document.getElementById('expense').requestSubmit();")
            waitFor(scenario, web, "document.querySelectorAll('#pending li').length", "1")
            eval(scenario, web, "document.getElementById('kind').value='income';document.getElementById('kind').dispatchEvent(new Event('change'));document.getElementById('description').value='Synthetic return income';document.getElementById('amount').value='2300,00';document.getElementById('date').value='2026-08-05';document.getElementById('expense').requestSubmit();")
            waitFor(scenario, web, "document.querySelectorAll('#pending li').length", "2")
            scenario.onActivity { web.destroy() }
        }
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            val web = load(scenario, "/app")
            waitFor(scenario, web, "typeof incomes", "\"object\"")
            val count = "(await incomes.listIncomes('$owner')).length+(await expenses.listExpenses('$owner')).length"
            eval(scenario, web, "(async()=>{window.remaining=$count;})()")
            waitFor(scenario, web, "window.remaining", "2")
            // The online synchronizers use navigator.onLine; all real traffic stays blocked.
            scenario.onActivity { web.setNetworkAvailable(true) }
            eval(scenario, web, "(async()=>{window.networkFailure=true;await Promise.all([syncAllForUser('$owner'),syncAllIncomesForUser('$owner')]);window.afterFailure=$count;})()")
            waitFor(scenario, web, "window.afterFailure", "2")
            assertEquals("0", eval(scenario, web, "writes.length"))
            eval(scenario, web, "(async()=>{window.networkFailure=false;await Promise.all([syncAllForUser('$owner'),syncAllForUser('$owner'),syncAllIncomesForUser('$owner'),syncAllIncomesForUser('$owner')]);window.remaining=$count;})()")
            waitFor(scenario, web, "window.remaining", "0")
            assertEquals("2", eval(scenario, web, "writes.length"))
            assertEquals("true", eval(scenario, web, "new Set(writes.map(x=>x.id)).size===2 && writes.every(x=>x.owner==='$owner' && x.actor==='$owner')"))
            assertEquals("true", eval(scenario, web, "writes.some(x=>x.type==='expense' && x.input.valor===85 && x.input.data==='2026-08-12') && writes.some(x=>x.type==='income' && x.input.valor===2300 && x.input.data==='2026-08-05')"))
            eval(scenario, web, "(async()=>{await Promise.all([syncAllForUser('$owner'),syncAllIncomesForUser('$owner')]);window.replayed=true;})()")
            waitFor(scenario, web, "window.replayed", "true")
            assertEquals("2", eval(scenario, web, "writes.length"))
            scenario.onActivity { web.destroy() }
        }
    }
}
