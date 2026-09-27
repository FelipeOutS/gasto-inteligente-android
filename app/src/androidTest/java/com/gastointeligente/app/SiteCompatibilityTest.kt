package com.gastointeligente.app

import android.Manifest
import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayInputStream
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = 26)
class SiteCompatibilityTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()

    private fun findWeb(view: View): WebView? {
        if (view is WebView) return view
        if (view is ViewGroup) for (index in 0 until view.childCount) {
            findWeb(view.getChildAt(index))?.let { return it }
        }
        return null
    }

    private fun web(activity: MainActivity) = requireNotNull(findWeb(activity.findViewById(android.R.id.content)))

    private fun loadFixture(scenario: ActivityScenario<MainActivity>) {
        val loaded = CountDownLatch(1)
        scenario.onActivity { activity ->
            web(activity).apply {
                stopLoading()
                webViewClient = object : WebViewClient() {
                    override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?) =
                        WebResourceResponse("text/html", "UTF-8", ByteArrayInputStream(
                            """<html><head><meta name="viewport" content="width=device-width,initial-scale=1,viewport-fit=cover"></head>
                            <body><label for="email">E-mail</label><input id="email" type="email" autocomplete="off"></body></html>""".toByteArray()
                        ))
                    override fun onPageFinished(view: WebView?, url: String?) { loaded.countDown() }
                }
                loadUrl("https://gastointeligente.com.br/android-compatibility-fixture")
            }
        }
        assertTrue("Fixture did not load", loaded.await(20, TimeUnit.SECONDS))
        instrumentation.waitForIdleSync()
    }

    @Test fun siteViewportStaysInsideSystemBarsAndCutout() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            loadFixture(scenario)
            scenario.onActivity { activity ->
                val view = web(activity)
                val root = activity.window.decorView
                val safe = requireNotNull(ViewCompat.getRootWindowInsets(root))
                    .getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
                val position = IntArray(2)
                view.getLocationOnScreen(position)
                assertTrue("Site overlaps the status bar", position[1] >= safe.top)
                assertTrue("Site overlaps bottom navigation", position[1] + view.height <= root.height - safe.bottom)
                assertTrue(view.height > root.height / 2)
            }
        }
    }

    @Test fun keyboardShrinksSiteViewportAndClosingItRestoresSpace() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            loadFixture(scenario)
            var initialHeight = 0
            val focused = CountDownLatch(1)
            scenario.onActivity { activity ->
                val view = web(activity)
                initialHeight = view.height
                view.requestFocus()
                view.evaluateJavascript("document.getElementById('email').focus()") { focused.countDown() }
            }
            assertTrue(focused.await(5, TimeUnit.SECONDS))
            scenario.onActivity { activity ->
                (activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
                    .showSoftInput(web(activity), InputMethodManager.SHOW_IMPLICIT)
            }
            var visible = false
            for (attempt in 0 until 100) {
                scenario.onActivity { activity ->
                    val root = activity.window.decorView
                    val insets = requireNotNull(ViewCompat.getRootWindowInsets(root))
                    visible = insets.isVisible(WindowInsetsCompat.Type.ime()) && web(activity).height < initialHeight
                }
                if (visible) break
                android.os.SystemClock.sleep(100)
            }
            assertTrue("Keyboard did not resize the site", visible)
            scenario.onActivity { activity ->
                val view = web(activity)
                val root = activity.window.decorView
                val ime = requireNotNull(ViewCompat.getRootWindowInsets(root)).getInsets(WindowInsetsCompat.Type.ime())
                val position = IntArray(2)
                view.getLocationOnScreen(position)
                assertTrue("Site extends under the keyboard", position[1] + view.height <= root.height - ime.bottom)
                (activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
                    .hideSoftInputFromWindow(view.windowToken, 0)
            }
            var restored = false
            for (attempt in 0 until 100) {
                scenario.onActivity { restored = web(it).height == initialHeight }
                if (restored) break
                android.os.SystemClock.sleep(100)
            }
            assertTrue("Site did not regain space after closing keyboard", restored)
        }
    }

    @Test fun websiteReceivesApproximateLocationPermissionWithoutPersistingOriginGrant() {
        val packageName = instrumentation.targetContext.packageName
        instrumentation.uiAutomation.executeShellCommand("pm grant $packageName ${Manifest.permission.ACCESS_COARSE_LOCATION}").close()
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            loadFixture(scenario)
            scenario.onActivity { activity ->
                val view = web(activity)
                var replies = 0
                view.webChromeClient!!.onGeolocationPermissionsShowPrompt("https://gastointeligente.com.br") { origin, allowed, retain ->
                    replies++
                    assertEquals("https://gastointeligente.com.br", origin)
                    assertTrue(allowed)
                    assertFalse(retain)
                }
                assertEquals(1, replies)
                view.webChromeClient!!.onGeolocationPermissionsShowPrompt("https://example.org") { _, allowed, _ ->
                    replies++
                    assertFalse(allowed)
                }
                assertEquals(2, replies)
            }
        }
    }
}
