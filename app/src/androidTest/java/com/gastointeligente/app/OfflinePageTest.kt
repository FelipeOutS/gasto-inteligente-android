package com.gastointeligente.app

import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import androidx.biometric.BiometricManager
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class OfflinePageTest {
    private fun findWeb(view: View): WebView? {
        if (view is WebView) return view
        if (view is ViewGroup) for (i in 0 until view.childCount) findWeb(view.getChildAt(i))?.let { return it }
        return null
    }
    private fun evaluate(scenario: ActivityScenario<MainActivity>, web: WebView, script: String): String {
        val latch = CountDownLatch(1)
        var value = ""
        scenario.onActivity { web.evaluateJavascript(script) { value = it; latch.countDown() } }
        assertTrue(latch.await(5, TimeUnit.SECONDS))
        return value
    }
    private fun waitFor(scenario: ActivityScenario<MainActivity>, web: WebView, expression: String, expected: String) {
        val deadline = android.os.SystemClock.elapsedRealtime() + 50000
        var actual = ""
        while (android.os.SystemClock.elapsedRealtime() < deadline) {
            actual = evaluate(scenario, web, expression)
            if (actual == expected) return
            android.os.SystemClock.sleep(150)
        }
        assertEquals("$expression; status=" + evaluate(scenario, web, "document.getElementById('message')?.textContent"), expected, actual)
    }
    @Test fun bundledScreenSavesWithoutNetworkSurvivesReloadAndSeparatesAccounts() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        assumeTrue("Enroll a strong biometric and send emulator finger touch 1 during this test",
            BiometricManager.from(context).canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS)
        val store = SecureSessionStore(context)
        fun saveIdentity(user: String) = store.save(JSONObject().put("access_token","offline-synthetic-access")
            .put("refresh_token","offline-synthetic-refresh").put("userId",user).put("email","$user@example.invalid"))
        store.clear()
        val accountA = "offline-${System.currentTimeMillis()}"
        saveIdentity(accountA)
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            lateinit var web: WebView
            scenario.onActivity { activity ->
                web = requireNotNull(findWeb(activity.findViewById(android.R.id.content)))
                // Block all network access for this WebView; intercepted assets must still work.
                web.settings.blockNetworkLoads = true
                web.loadUrl(OfflinePage.URL)
            }
            try {
                fun unlock() {
                    waitFor(scenario, web, "typeof AndroidSecureSession", "\"object\"")
                    waitFor(scenario, web, "document.readyState === 'complete' && typeof document.getElementById('unlock')?.onclick === 'function'", "true")
                    evaluate(scenario, web, "window.fixtureLeak = false; window.addEventListener('AndroidSecureSessionResult', e => {window.fixtureLeak = !!e.detail.access_token || !!e.detail.refresh_token;}, {once:true}); document.getElementById('unlock').click();")
                    waitFor(scenario, web, "document.getElementById('content')?.hidden", "false")
                    assertEquals("false", evaluate(scenario,web,"window.fixtureLeak"))
                }
                unlock()
                evaluate(scenario,web,"document.getElementById('description').value='Almoço offline'; document.getElementById('amount').value='12,50'; document.getElementById('expense').requestSubmit(); document.getElementById('expense').requestSubmit();")
                waitFor(scenario,web,"document.querySelectorAll('#pending li').length","1")
                scenario.onActivity { web.reload() }
                waitFor(scenario,web,"document.getElementById('content')?.hidden","true")
                unlock()
                waitFor(scenario,web,"document.querySelectorAll('#pending li').length","1")
                assertEquals("true",evaluate(scenario,web,"document.getElementById('pending').textContent.includes('Almoço offline')"))
                saveIdentity("other-$accountA")
                scenario.onActivity { web.reload() }
                waitFor(scenario,web,"document.getElementById('content')?.hidden","true")
                unlock()
                waitFor(scenario,web,"document.getElementById('count').textContent.startsWith('0 ')","true")
                assertEquals("0",evaluate(scenario,web,"document.querySelectorAll('#pending li').length"))
            } finally { store.clear() }
        }
    }
}
