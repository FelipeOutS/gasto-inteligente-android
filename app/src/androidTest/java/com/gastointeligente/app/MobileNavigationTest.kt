package com.gastointeligente.app

import android.graphics.Bitmap
import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayInputStream
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/** Tests the real BottomNav bundle in MainActivity's WebView, NOT an authenticated
 * Dashboard. The fixture has no session/financial data and is packaged only in the test APK. */
@RunWith(AndroidJUnit4::class)
class MobileNavigationTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private fun findWeb(view: View): WebView? {
        if (view is WebView) return view
        if (view is ViewGroup) for (i in 0 until view.childCount) findWeb(view.getChildAt(i))?.let { return it }
        return null
    }
    private fun web(activity: MainActivity) = requireNotNull(findWeb(activity.findViewById(android.R.id.content)))
    private fun eval(scenario: ActivityScenario<MainActivity>, script: String): String {
        val latch = CountDownLatch(1)
        var result = "null"
        scenario.onActivity { web(it).evaluateJavascript(script) { value -> result = value; latch.countDown() } }
        assertTrue("JS timed out", latch.await(10, TimeUnit.SECONDS))
        return if (result.startsWith("\"")) JSONArray("[$result]").getString(0) else result
    }
    private fun waitFor(scenario: ActivityScenario<MainActivity>, script: String) {
        val end = SystemClock.uptimeMillis() + 10000
        while (SystemClock.uptimeMillis() < end) {
            if (eval(scenario, script) == "true") return
            SystemClock.sleep(40)
        }
        screenshot("fixture-timeout.png")
        fail("Condition not reached: $script; state=" + eval(scenario, "JSON.stringify({url:location.pathname,focus:document.activeElement?.id,height:innerHeight,visual:visualViewport.height,hidden:document.querySelector('.mobile-bottom-nav')?.hidden})"))
    }
    private fun loadFixture(scenario: ActivityScenario<MainActivity>) {
        val html = instrumentation.context.assets.open("mobile-navigation/navigation.html").use { it.readBytes() }
        val loaded = CountDownLatch(1)
        scenario.onActivity { activity -> web(activity).apply {
            stopLoading()
            webViewClient = object : WebViewClient() {
                override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?) = WebResourceResponse("text/html", "UTF-8", ByteArrayInputStream(html))
                override fun onPageFinished(view: WebView?, url: String?) { loaded.countDown() }
            }
            loadUrl("https://navigation-fixture.invalid/app")
        } }
        assertTrue(loaded.await(20, TimeUnit.SECONDS))
        waitFor(scenario, "!!document.querySelector('.mobile-bottom-nav a')")
    }
    private fun tap(scenario: ActivityScenario<MainActivity>, selector: String) {
        val encoded = JSONObject.quote(selector)
        val point = JSONObject(eval(scenario, "JSON.stringify((()=>{const e=document.querySelector($encoded);const r=e.getBoundingClientRect();return {x:r.x+r.width/2,y:r.y+r.height/2,width:innerWidth};})())"))
        var x = 0f; var y = 0f
        scenario.onActivity {
            val w = web(it); val location = IntArray(2); w.getLocationOnScreen(location)
            val scale = w.width / point.getDouble("width")
            x = (location[0] + point.getDouble("x") * scale).toFloat()
            y = (location[1] + point.getDouble("y") * scale).toFloat()
        }
        val time = SystemClock.uptimeMillis()
        val finger = MotionEvent.PointerProperties().apply { id = 0; toolType = MotionEvent.TOOL_TYPE_FINGER }
        val coords = MotionEvent.PointerCoords().apply { this.x = x; this.y = y; pressure = 1f; size = 1f }
        fun event(action: Int, at: Long) = MotionEvent.obtain(time, at, action, 1, arrayOf(finger), arrayOf(coords), 0, 0, 1f, 1f, 0, 0, android.view.InputDevice.SOURCE_TOUCHSCREEN, 0)
        instrumentation.sendPointerSync(event(MotionEvent.ACTION_DOWN, time))
        instrumentation.sendPointerSync(event(MotionEvent.ACTION_UP, time + 40))
        instrumentation.waitForIdleSync()
    }
    private fun screenshot(name: String) {
        instrumentation.waitForIdleSync()
        SystemClock.sleep(250) // Let the compositor present the verified scroll/keyboard frame.
        val dir = File(instrumentation.targetContext.getExternalFilesDir(null), "mobile-navigation").apply { mkdirs() }
        val bitmap = instrumentation.uiAutomation.takeScreenshot()
        File(dir, name).outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
    }
    private fun route(scenario: ActivityScenario<MainActivity>, path: String) = waitFor(scenario, "location.pathname === ${JSONObject.quote(path)} && document.querySelector('#route-path').textContent === ${JSONObject.quote(path)}")
    private fun settled(scenario: ActivityScenario<MainActivity>) = waitFor(scenario, "document.getAnimations().every(a => a.playState !== 'running')")

    @Test fun onePhysicalTapPerTabAndRapidAlternation() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            loadFixture(scenario); settled(scenario); screenshot("fixture-dashboard-dark.png")
            for (path in listOf("/gastos", "/cartoes", "/app/cofre-pessoal", "/app")) {
                tap(scenario, ".mobile-bottom-nav a[href='$path']"); route(scenario, path)
                assertEquals(path, eval(scenario, "document.querySelector('.mobile-bottom-nav [aria-current=page]').getAttribute('href')"))
                if (path == "/gastos") { settled(scenario); screenshot("fixture-gastos-dark.png") }
            }
            repeat(3) { for (path in listOf("/gastos", "/cartoes", "/app/cofre-pessoal", "/app")) {
                tap(scenario, ".mobile-bottom-nav a[href='$path']"); route(scenario, path)
            } }
        }
    }
    @Test fun centralActionOpensCurrentExpenseAndIncomeDestinations() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            loadFixture(scenario)
            for ((index, path) in listOf("/manual", "/renda/nova").withIndex()) {
                tap(scenario, ".mobile-nav-create")
                waitFor(scenario, "!!document.querySelector('[role=dialog][data-state=open]')")
                tap(scenario, "[role=dialog] .grid > button:nth-child(${index + 1})")
                route(scenario, path)
                assertEquals("0", eval(scenario, "document.querySelectorAll('.mobile-bottom-nav [aria-current=page]').length"))
            }
        }
    }

    @Test fun fourteenDestinationsHistoryRefreshAndTargets() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            loadFixture(scenario)
            assertEquals("true", eval(scenario, "[...document.querySelectorAll('.mobile-bottom-nav a')].every(e=>{const r=e.getBoundingClientRect();return r.width>=44&&r.height>=44})"))
            for (path in listOf("/gastos", "/renda", "/contas-a-pagar", "/contas-a-receber", "/cartoes", "/assinaturas", "/orcamento", "/guardado", "/metas", "/relatorios", "/mercado", "/bens", "/app")) {
                eval(scenario, """document.querySelector("#fixture-more").scrollIntoView({block:"center"})""");
                tap(scenario, "#fixture-more"); route(scenario, "/app/mais")
                eval(scenario, "document.querySelector(\"#more-links a[href='$path']\").scrollIntoView({block:'center',behavior:'instant'})")
                tap(scenario, "#more-links a[href='$path']"); route(scenario, path)
            }
            scenario.onActivity { web(it).goBack() }; route(scenario, "/app/mais")
            scenario.onActivity { web(it).goForward() }; route(scenario, "/app")
            scenario.onActivity { web(it).reload() }; route(scenario, "/app")
        }
    }
    @Test fun scrollLightThemeAndLastButtonStayReachable() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            loadFixture(scenario)
            tap(scenario, "#theme"); waitFor(scenario, "document.documentElement.classList.contains('light')")
            settled(scenario); screenshot("fixture-dashboard-light.png")
            eval(scenario, "window.scrollTo(0, document.documentElement.scrollHeight)")
            waitFor(scenario, "window.scrollY > 500")
            assertEquals("true", eval(scenario, "document.querySelector('#last-item').getBoundingClientRect().bottom < document.querySelector('.mobile-bottom-nav').getBoundingClientRect().top"))
            assertEquals("false", eval(scenario, "document.querySelector('.mobile-bottom-nav').hidden"))
            screenshot("fixture-scroll.png")
            tap(scenario, ".mobile-bottom-nav a[href='/gastos']"); route(scenario, "/gastos")
        }
    }
    @Test fun keyboardHidesNavigationAndFirstTapWorksAfterClosing() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            loadFixture(scenario)
            for (path in listOf("/gastos", "/renda", "/contas-a-pagar", "/contas-a-receber")) {
                // Each fixture destination has the same real editable search field.
                eval(scenario, "document.querySelector(\"#fixture-more\").click()")
                route(scenario, "/app/mais")
                eval(scenario, "document.querySelector(\"#more-links a[href='$path']\").click()")
                route(scenario, path)
                eval(scenario, "document.querySelector('#search').scrollIntoView({block:'center'})")
                tap(scenario, "#search")
                waitFor(scenario, "document.activeElement.id === 'search'")
                // The AVD exposes a hardware keyboard. Explicitly request the software IME
                // for this test without changing the user's keyboard settings.
                scenario.onActivity { activity ->
                    (activity.getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager).showSoftInput(web(activity), android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
                }
                // API 37/Gboard may open its first-use stylus tutorial instead of
                // the IME. Dismiss only that external setup screen; keep all viewport assertions.
                SystemClock.sleep(500)
                val root = instrumentation.uiAutomation.rootInActiveWindow
                if (root?.findAccessibilityNodeInfosByText("Try out your stylus")?.isNotEmpty() == true) {
                    root.findAccessibilityNodeInfosByText("Cancel").firstOrNull()?.performAction(android.view.accessibility.AccessibilityNodeInfo.ACTION_CLICK)
                    instrumentation.waitForIdleSync()
                    scenario.onActivity { activity ->
                        (activity.getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager).showSoftInput(web(activity), android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
                    }
                }
                waitFor(scenario, "document.querySelector('.mobile-bottom-nav').hidden")
                if (path == "/gastos") screenshot("fixture-keyboard.png")
                instrumentation.sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK)
                waitFor(scenario, "!document.querySelector('.mobile-bottom-nav').hidden")
                tap(scenario, ".mobile-bottom-nav a[href='/app']"); route(scenario, "/app")
            }
        }
    }
}
