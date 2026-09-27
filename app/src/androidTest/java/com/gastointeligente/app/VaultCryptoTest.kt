package com.gastointeligente.app

import android.content.Context
import android.security.keystore.KeyInfo
import android.util.Base64
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
import java.security.KeyFactory
import java.security.KeyStore
import java.security.MessageDigest
import java.security.PrivateKey
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/** Synthetic fixtures. Approval requires a real emulator fingerprint input.
 * No mocked AuthenticationResult and no weakened production key policy. */
@RunWith(AndroidJUnit4::class)
class VaultCryptoTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val userId = "vault-crypto-fixture"
    private val raw = Base64.encodeToString(ByteArray(32) { (it + 1).toByte() }, Base64.NO_WRAP)
    private val scope = MessageDigest.getInstance("SHA-256").digest(userId.toByteArray()).joinToString("") { "%02x".format(it) }
    private fun payload() = JSONObject().put("userId", userId).put("master_key", raw)
    private fun requireBiometric() = assumeTrue("Strong biometric must be enrolled",
        BiometricManager.from(context).canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS)

    @Test fun keyRequiresPerUseAuthenticationAndDiskDoesNotContainMasterKey() {
        requireBiometric()
        val store = SecureSessionStore(context, userId); store.clear()
        try {
            store.save(payload())
            val disk = context.getSharedPreferences("secure-vault-v2-$scope", Context.MODE_PRIVATE).all.toString()
            assertFalse(disk.contains(raw)); assertFalse(disk.contains("master_key")); assertFalse(disk.contains("localKey"))
            val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
            val key = keyStore.getKey("gi.vault.biometric.v2.$scope", null) as PrivateKey
            assertNull(key.encoded)
            val info = KeyFactory.getInstance(key.algorithm, "AndroidKeyStore").getKeySpec(key, KeyInfo::class.java)
            assertTrue(info.isUserAuthenticationRequired)
            assertTrue(info.isInvalidatedByBiometricEnrollment)
            // Modern auth-per-operation API reports 0; legacy API reports -1.
            assertEquals(if (android.os.Build.VERSION.SDK_INT >= 30) 0 else -1,
                info.userAuthenticationValidityDurationSeconds)
            var rejected = false
            try { val locked = store.prepareUnlock(); store.unlock(locked, locked.cipher) }
            catch (_: Exception) { rejected = true }
            assertTrue("Vault key decrypted without biometric authorization", rejected)
        } finally { store.clear() }
    }
    @Test fun vaultNamespacesDoNotOverwriteLoginOrOtherUsers() {
        requireBiometric()
        val a = SecureSessionStore(context, userId)
        val b = SecureSessionStore(context, "other-vault-fixture")
        val prefs = context.getSharedPreferences("secure-session-v1", Context.MODE_PRIVATE)
        val before = prefs.all.toMap()
        try {
            a.save(payload())
            b.save(JSONObject().put("userId", "other-vault-fixture").put("master_key", raw))
            a.clear()
            assertFalse(a.hasSession()); assertTrue(b.hasSession()); assertEquals(before, prefs.all)
        } finally { a.clear(); b.clear() }
    }
    @Test fun corruptKeyPayloadIsRejectedBeforePersistence() {
        val store = SecureSessionStore(context, userId); store.clear()
        for (data in listOf(payload().put("master_key", "invalid"), payload().put("userId", "wrong-user"))) {
            var rejected = false
            try { store.save(data) } catch (_: Exception) { rejected = true }
            assertTrue(rejected); assertFalse(store.hasSession())
        }
    }
    @Test fun restoredCiphertextWithoutKeystoreKeyCannotUnlock() {
        requireBiometric()
        val store = SecureSessionStore(context, userId); store.clear()
        try {
            store.save(payload())
            KeyStore.getInstance("AndroidKeyStore").apply { load(null); deleteEntry("gi.vault.biometric.v2.$scope") }
            assertFalse(store.hasSession())
            var rejected = false
            try { store.prepareUnlock() } catch (_: Exception) { rejected = true }
            assertTrue(rejected)
        } finally { store.clear() }
    }

    private fun runBridge(script: String, cancelPrompt: Boolean = false) {
        requireBiometric()
        val store = SecureSessionStore(context, userId); store.clear()
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            lateinit var web: WebView
            lateinit var bridge: WebSecureSession
            val loaded = CountDownLatch(1)
            scenario.onActivity { activity ->
                web = WebView(activity); web.settings.javaScriptEnabled = true
                bridge = WebSecureSession(activity, web); assertTrue(bridge.attach())
                web.webViewClient = object : WebViewClient() {
                    override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?) =
                        WebResourceResponse("text/html", "UTF-8", ByteArrayInputStream("<html>Vault fixture</html>".toByteArray()))
                    override fun onPageFinished(view: WebView?, url: String?) { loaded.countDown() }
                }
                activity.setContentView(web); web.loadUrl("https://gastointeligente.com.br/vault-crypto-fixture")
            }
            try {
                assertTrue(loaded.await(20, TimeUnit.SECONDS))
                scenario.onActivity { web.evaluateJavascript("window.fixtureResult='waiting';" + script, null) }
                if (cancelPrompt) {
                    android.os.SystemClock.sleep(1500)
                    InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand("input keyevent 4").close()
                }
                var result = "\"waiting\""
                val deadline = android.os.SystemClock.elapsedRealtime() + 95000
                while (result == "\"waiting\"" && android.os.SystemClock.elapsedRealtime() < deadline) {
                    val evaluated = CountDownLatch(1)
                    scenario.onActivity { web.evaluateJavascript("window.fixtureResult") { result = it; evaluated.countDown() } }
                    assertTrue(evaluated.await(3, TimeUnit.SECONDS))
                    if (result == "\"waiting\"") android.os.SystemClock.sleep(200)
                }
                assertEquals("\"passed\"", result)
                if (cancelPrompt) assertFalse(store.hasSession())
            } finally {
                scenario.onActivity { bridge.close(); (web.parent as? ViewGroup)?.removeView(web); web.destroy() }
                store.clear()
            }
        }
    }
    @Test fun enrollmentAndUnlockRequireApprovedCryptoOperations() {
        runBridge("""
            (async () => {
              const enrolled = await NativeVault.enroll('$userId', '$raw');
              if (!enrolled.success || enrolled.master_key !== '$raw') { fixtureResult='enrollment-failed'; return; }
              const opened = await NativeVault.unlock('$userId');
              fixtureResult = opened.success && opened.master_key === '$raw' && opened.userId === '$userId' ? 'passed' : 'unlock-failed';
            })().catch(() => fixtureResult='error');
        """.trimIndent())
    }
    @Test fun cancelingEnrollmentReturnsNoKeyAndRemovesUnconfirmedEnvelope() {
        runBridge("""
            NativeVault.enroll('$userId', '$raw').then(result => {
              fixtureResult = !result.success && !result.master_key ? 'passed' : 'unexpected-key';
            });
        """.trimIndent(), cancelPrompt = true)
    }
}
