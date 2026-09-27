package com.gastointeligente.app

import android.content.Intent
import android.net.Uri
import android.view.View
import android.view.ViewGroup
import android.view.KeyEvent
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.io.ByteArrayInputStream

@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = 26)
class MainActivityTest {
    private fun findWebView(view: View): WebView? {
        if (view is WebView) return view
        if (view is ViewGroup) {
            for (index in 0 until view.childCount) findWebView(view.getChildAt(index))?.let { return it }
        }
        return null
    }

    private fun webView(activity: MainActivity): WebView =
        requireNotNull(findWebView(activity.findViewById(android.R.id.content)))

    private fun imageInput() = object : WebChromeClient.FileChooserParams() {
        override fun getMode() = MODE_OPEN
        override fun getAcceptTypes() = arrayOf("image/*", "application/pdf")
        override fun isCaptureEnabled() = false
        override fun getTitle(): CharSequence? = null
        override fun getFilenameHint(): String? = null
        override fun createIntent() = Intent(Intent.ACTION_GET_CONTENT)
    }

    @Test fun recreationRestoresCurrentPageInsteadOfReturningToAppRoot() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            InstrumentationRegistry.getInstrumentation().waitForIdleSync()
            val loaded = CountDownLatch(1)
            val fixtureUrl = "https://gastointeligente.com.br/android-local-test"
            scenario.onActivity { activity ->
                webView(activity).apply {
                    stopLoading()
                    webViewClient = object : WebViewClient() {
                        override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse =
                            WebResourceResponse("text/html", "UTF-8",
                                ByteArrayInputStream("<html><body>Teste local</body></html>".toByteArray()))

                        override fun onPageFinished(view: WebView?, url: String?) {
                            if (url == fixtureUrl) loaded.countDown()
                        }
                    }
                    // Inline fixture: no login, uploaded receipt, or backend mutation.
                    loadUrl(fixtureUrl)
                }
            }
            assertTrue("Local fixture did not load", loaded.await(20, TimeUnit.SECONDS))
            scenario.recreate()
            InstrumentationRegistry.getInstrumentation().waitForIdleSync()
            scenario.onActivity { activity ->
                assertEquals(fixtureUrl, webView(activity).copyBackForwardList().currentItem?.url)
            }
        }
    }

    @Test fun cancelingSourceDialogCompletesUploadOnce() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            InstrumentationRegistry.getInstrumentation().waitForIdleSync()
            var completions = 0
            val completed = CountDownLatch(1)
            var result: Array<Uri>? = arrayOf(Uri.EMPTY)
            scenario.onActivity { activity ->
                val web = webView(activity)
                assertTrue(web.webChromeClient!!.onShowFileChooser(web, ValueCallback {
                    completions++
                    result = it
                    completed.countDown()
                }, imageInput()))
            }
            InstrumentationRegistry.getInstrumentation().waitForIdleSync()
            InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
            assertTrue("Upload cancellation was not delivered", completed.await(10, TimeUnit.SECONDS))
            scenario.onActivity {
                assertEquals(1, completions)
                assertNull(result)
            }
        }
    }

    @Test fun secondRequestCannotReplaceAnExistingUploadCallback() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            InstrumentationRegistry.getInstrumentation().waitForIdleSync()
            var firstCompletions = 0
            var secondCompletions = 0
            val firstCompleted = CountDownLatch(1)
            scenario.onActivity { activity ->
                val web = webView(activity)
                web.webChromeClient!!.onShowFileChooser(web, ValueCallback {
                    firstCompletions++
                    firstCompleted.countDown()
                }, imageInput())
                web.webChromeClient!!.onShowFileChooser(web, ValueCallback {
                    secondCompletions++
                    assertNull(it)
                }, imageInput())
                assertEquals(0, firstCompletions)
                assertEquals(1, secondCompletions)
            }
            InstrumentationRegistry.getInstrumentation().waitForIdleSync()
            InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
            assertTrue("Original upload cancellation was not delivered", firstCompleted.await(10, TimeUnit.SECONDS))
            scenario.onActivity {
                assertEquals(1, firstCompletions)
                assertEquals(1, secondCompletions)
            }
        }
    }
}
