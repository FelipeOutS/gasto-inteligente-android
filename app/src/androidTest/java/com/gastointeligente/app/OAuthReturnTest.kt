package com.gastointeligente.app

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.View
import android.view.ViewGroup
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
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
class OAuthReturnTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private fun findWeb(view: View): WebView? {
        if (view is WebView) return view
        if (view is ViewGroup) for (index in 0 until view.childCount) findWeb(view.getChildAt(index))?.let { return it }
        return null
    }

    @Test fun verifiedAttemptReturnsToExistingActivityAndCannotBeReplayed() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            val loaded = CountDownLatch(1)
            val callbackLoaded = CountDownLatch(1)
            var callbackCount = 0
            scenario.onActivity { activity ->
                activity.getSharedPreferences("android-oauth", Context.MODE_PRIVATE).edit()
                    .putString("state", "test-state").putLong("startedAt", System.currentTimeMillis()).commit()
                val web = requireNotNull(findWeb(activity.findViewById(android.R.id.content)))
                web.stopLoading()
                web.webViewClient = object : WebViewClient() {
                    override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?) =
                        WebResourceResponse("text/html", "UTF-8", ByteArrayInputStream("<html>Local OAuth fixture; no SDK or network</html>".toByteArray()))
                    override fun onPageFinished(view: WebView?, url: String?) {
                        if (url?.contains("/auth/android/callback") == true) { callbackCount++; callbackLoaded.countDown() }
                        else loaded.countDown()
                    }
                }
                web.loadUrl("https://gastointeligente.com.br/android-oauth-fixture")
            }
            assertTrue(loaded.await(20, TimeUnit.SECONDS))
            fun returnToApp() {
                instrumentation.targetContext.startActivity(Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://gastointeligente.com.br/auth/android/callback?request=test-state#access_token=synthetic-access&refresh_token=synthetic-refresh&expires_in=3600&token_type=bearer"),
                    instrumentation.targetContext, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
            returnToApp()
            assertTrue(callbackLoaded.await(20, TimeUnit.SECONDS))
            scenario.onActivity { activity ->
                assertNull(activity.getSharedPreferences("android-oauth", Context.MODE_PRIVATE).getString("state", null))
                assertNull(activity.intent.data)
                assertEquals(1, callbackCount)
            }
            returnToApp()
            instrumentation.waitForIdleSync()
            scenario.onActivity { assertEquals("A consumed callback must not load another session", 1, callbackCount) }
        }
    }
}
