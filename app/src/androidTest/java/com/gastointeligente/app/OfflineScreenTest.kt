package com.gastointeligente.app

import android.net.Uri
import android.view.View
import android.view.ViewGroup
import android.webkit.WebResourceResponse
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.*

@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = 26)
class OfflineScreenTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    private fun findWebView(view: View): WebView? {
        if (view is WebView) return view
        if (view is ViewGroup) for (index in 0 until view.childCount) {
            findWebView(view.getChildAt(index))?.let { return it }
        }
        return null
    }

    private fun request(mainFrame: Boolean) = object : WebResourceRequest {
        override fun getUrl() = Uri.parse("https://gastointeligente.com.br/app")
        override fun isForMainFrame() = mainFrame
        override fun isRedirect() = false
        override fun hasGesture() = false
        override fun getMethod() = "GET"
        override fun getRequestHeaders() = emptyMap<String, String>()
    }

    private fun serverFailure(mainFrame: Boolean) {
        compose.runOnUiThread {
            val web = requireNotNull(findWebView(compose.activity.findViewById(android.R.id.content)))
            val client = web.webViewClient
            web.stopLoading()
            // Isolate callback behavior from the real site's initial request.
            web.webViewClient = WebViewClient()
            web.loadDataWithBaseURL("https://gastointeligente.com.br", "<html></html>", "text/html", "UTF-8", null)
            client.onPageStarted(web, "https://gastointeligente.com.br/app", null)
            client.onReceivedHttpError(web, request(mainFrame),
                WebResourceResponse("text/html", "UTF-8", 503, "Service Unavailable", emptyMap(), null))
        }
    }

    @Test fun pageServerFailureOffersRetry() {
        serverFailure(true)
        compose.onNodeWithText("Tentar novamente").assertIsDisplayed()
    }

    @Test fun failedSubresourceDoesNotReplaceThePage() {
        serverFailure(false)
        compose.onNodeWithText("Tentar novamente").assertDoesNotExist()
    }

    @Test fun rendererCrashKeepsActivityAliveAndRetryCreatesNewWebView() {
        lateinit var oldWeb: WebView
        compose.runOnIdle {
            oldWeb = requireNotNull(findWebView(compose.activity.findViewById(android.R.id.content)))
            oldWeb.loadUrl("chrome://crash")
        }
        compose.waitUntil(20000) { compose.onAllNodes(androidx.compose.ui.test.hasText("Tentar novamente")).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Tentar novamente").performClick()
        compose.runOnIdle {
            val replacement = requireNotNull(findWebView(compose.activity.findViewById(android.R.id.content)))
            assertNotSame(oldWeb, replacement)
            assertFalse(compose.activity.isFinishing)
        }
    }
}
