package com.gastointeligente.app

import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import android.view.ViewGroup
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayInputStream
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class WebSecureSessionTest {
    private fun evaluate(url: String, expression: String): String {
        val loaded = CountDownLatch(1)
        val completed = CountDownLatch(1)
        var result = ""
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            lateinit var web: WebView
            lateinit var bridge: WebSecureSession
            scenario.onActivity { activity ->
                web = WebView(activity)
                web.settings.javaScriptEnabled = true
                bridge = WebSecureSession(activity, web)
                assertTrue(bridge.attach())
                web.webViewClient = object : WebViewClient() {
                    override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?) =
                        WebResourceResponse("text/html", "UTF-8", ByteArrayInputStream("<html><body>Isolated fixture</body></html>".toByteArray()))
                    override fun onPageFinished(view: WebView?, page: String?) { if (page == url) loaded.countDown() }
                }
                activity.setContentView(web)
                web.loadUrl(url)
            }
            try {
                assertTrue(loaded.await(20, TimeUnit.SECONDS))
                scenario.onActivity { web.evaluateJavascript(expression) { result = it; completed.countDown() } }
                assertTrue(completed.await(5, TimeUnit.SECONDS))
            } finally {
                scenario.onActivity { bridge.close(); (web.parent as? ViewGroup)?.removeView(web); web.destroy() }
            }
        }
        return result
    }

    @Test fun trustedMainPageReceivesCompatibleApi() {
        assertEquals("true", evaluate("https://gastointeligente.com.br/secure-fixture",
            "['saveSession','hasSession','getSavedEmail','unlockSession','clearSession'].every(k => typeof AndroidSecureSession[k] === 'function')"))
    }
    @Test fun secureUnlockReturnsCorrelatedPromise() {
        assertEquals("true", evaluate("https://gastointeligente.com.br/secure-fixture",
            "typeof AndroidSecureSession.unlockSession().then === 'function'"))
    }
    @Test fun externalPageHasNoSessionChannel() {
        assertEquals("true", evaluate("https://example.org/secure-fixture", "typeof AndroidSecureSession === 'undefined' && typeof GastoSecureSession === 'undefined'"))
    }
    @Test fun trustedPageReceivesVaultContractOnSameChannel() {
        assertEquals("true", evaluate("https://gastointeligente.com.br/secure-fixture",
            "NativeVault.version === 2 && ['status','enroll','unlock','clear'].every(k => typeof NativeVault[k] === 'function')"))
    }
    @Test fun untrustedSubdomainHasNoVaultChannel() {
        assertEquals("true", evaluate("https://untrusted.gastointeligente.com.br/secure-fixture",
            "typeof NativeVault === 'undefined' && typeof GastoSecureSession === 'undefined'"))
    }
    @Test fun clearIsIdempotentAndDoesNotLeaveTokens() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val store = SecureSessionStore(context)
        store.clear(); store.clear()
        assertFalse(store.hasSession())
        assertTrue(context.getSharedPreferences("secure-session-v1", 0).all.isEmpty())
    }
    @Test fun invalidSessionIsRejectedBeforeCreatingKeyOrWritingTokens() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val store = SecureSessionStore(context)
        store.clear()
        try { store.save(org.json.JSONObject().put("access_token", "")); fail("Accepted invalid session") }
        catch (_: IllegalArgumentException) { }
        assertFalse(store.hasSession())
        assertTrue(context.getSharedPreferences("secure-session-v1", 0).all.isEmpty())
    }
}
