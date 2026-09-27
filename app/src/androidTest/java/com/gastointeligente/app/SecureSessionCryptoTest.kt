package com.gastointeligente.app

import android.content.Context
import android.view.ViewGroup
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.biometric.BiometricManager
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayInputStream
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/** Requires an enrolled strong biometric. In the dedicated emulator, send `adb emu finger touch 1`.
 * Never weakens the production key policy or replaces successful authentication with a mock. */
@RunWith(AndroidJUnit4::class)
class SecureSessionCryptoTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private fun requireBiometric() = assumeTrue("Enroll a strong biometric for crypto integration tests",
        BiometricManager.from(context).canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS)
    private fun payload() = JSONObject().put("access_token", "synthetic-private-access")
        .put("refresh_token", "synthetic-private-refresh").put("userId", "synthetic-user").put("email", "fixture@example.invalid")

    @Test fun savedTokensAreEncryptedAndCannotBeUnwrappedWithoutBiometric() {
        requireBiometric()
        val store = SecureSessionStore(context)
        store.clear()
        try {
            store.save(payload())
            assertTrue(store.hasSession())
            val disk = context.getSharedPreferences("secure-session-v1", Context.MODE_PRIVATE).all.toString()
            assertFalse(disk.contains("synthetic-private"))
            assertFalse(disk.contains("fixture@example.invalid"))
            var rejected = false
            try { val locked = store.prepareUnlock(); store.unlock(locked, locked.cipher) }
            catch (_: Exception) { rejected = true }
            assertTrue("Keystore decrypted without biometric authorization", rejected)
        } finally { store.clear() }
    }

    @Test fun authenticatedNativeBridgeReturnsExactTokens() {
        requireBiometric()
        val store = SecureSessionStore(context)
        store.clear()
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            lateinit var web: WebView
            lateinit var bridge: WebSecureSession
            val loaded = CountDownLatch(1)
            scenario.onActivity { activity ->
                web = WebView(activity)
                web.settings.javaScriptEnabled = true
                bridge = WebSecureSession(activity, web)
                assertTrue(bridge.attach())
                web.webViewClient = object : WebViewClient() {
                    override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?) =
                        WebResourceResponse("text/html", "UTF-8", ByteArrayInputStream("<html>Biometric fixture</html>".toByteArray()))
                    override fun onPageFinished(view: WebView?, url: String?) { loaded.countDown() }
                }
                activity.setContentView(web)
                web.loadUrl("https://gastointeligente.com.br/secure-crypto-fixture")
            }
            try {
                assertTrue(loaded.await(20, TimeUnit.SECONDS))
                scenario.onActivity {
                    web.evaluateJavascript("""
                        window.fixtureResult = 'waiting';
                        window.addEventListener('AndroidSecureSessionResult', e => {
                          fixtureResult = e.detail.success && e.detail.access_token === 'synthetic-private-access'
                            && e.detail.refresh_token === 'synthetic-private-refresh' ? 'passed' : 'failed';
                        }, {once:true});
                        AndroidSecureSession.saveSession('synthetic-private-access','synthetic-private-refresh','synthetic-user','fixture@example.invalid')
                          .then(ok => { if (ok) AndroidSecureSession.unlockSession(); else fixtureResult = 'save-failed'; });
                    """.trimIndent(), null)
                }
                var result = "\"waiting\""
                val deadline = android.os.SystemClock.elapsedRealtime() + 50000
                while (result == "\"waiting\"" && android.os.SystemClock.elapsedRealtime() < deadline) {
                    val evaluated = CountDownLatch(1)
                    scenario.onActivity { web.evaluateJavascript("window.fixtureResult") { result = it; evaluated.countDown() } }
                    assertTrue(evaluated.await(3, TimeUnit.SECONDS))
                    if (result == "\"waiting\"") android.os.SystemClock.sleep(250)
                }
                assertEquals("\"passed\"", result)
            } finally {
                scenario.onActivity { bridge.close(); (web.parent as? ViewGroup)?.removeView(web); web.destroy() }
                store.clear()
            }
        }
    }
}
