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
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/** Synthetic identity, isolated origin, real assets and IndexedDB. Not a biometric test. */
@RunWith(AndroidJUnit4::class)
class OfflineFormPersistenceTest {
    private fun eval(scenario: ActivityScenario<MainActivity>, web: WebView, script: String): String {
        val latch=CountDownLatch(1); var result=""
        scenario.onActivity { web.evaluateJavascript(script) { result=it; latch.countDown() } }
        assertTrue(latch.await(5,TimeUnit.SECONDS)); return result
    }
    private fun waitFor(scenario: ActivityScenario<MainActivity>, web: WebView, expression:String, expected:String) {
        val deadline=android.os.SystemClock.elapsedRealtime()+15000
        var actual=""
        while(android.os.SystemClock.elapsedRealtime()<deadline) {
            actual=eval(scenario,web,expression)
            if(actual==expected)return
            android.os.SystemClock.sleep(100)
        }
        assertEquals(expression,expected,actual)
    }
    private fun load(scenario: ActivityScenario<MainActivity>, owner:String): WebView {
        val context=InstrumentationRegistry.getInstrumentation().targetContext
        val html=context.assets.open("offline.html").bufferedReader().use { it.readText() }
        lateinit var web: WebView
        scenario.onActivity { activity ->
            // Dedicated WebView. Never reads the normal app origin or its session.
            web=WebView(activity)
            web.settings.javaScriptEnabled=true
            web.settings.domStorageEnabled=true
            web.settings.blockNetworkLoads=true
            activity.setContentView(web)
            web.webViewClient=object: WebViewClient() {
                override fun shouldInterceptRequest(view:WebView?,request:WebResourceRequest?):WebResourceResponse? {
                    return if(request?.url?.toString()=="https://offline-form.example.invalid/__android/offline.js")
                        WebResourceResponse("text/javascript","UTF-8",context.assets.open("offline.js")) else null
                }
            }
            web.loadDataWithBaseURL("https://offline-form.example.invalid/__android/offline",html,"text/html","UTF-8",null)
        }
        waitFor(scenario,web,"typeof document.getElementById('expense')?.onsubmit","\"function\"")
        eval(scenario,web,"window.dispatchEvent(new CustomEvent('AndroidSecureSessionResult',{detail:{success:true,user_id:'$owner'}}))")
        waitFor(scenario,web,"document.getElementById('content').hidden","false")
        return web
    }
    @Test fun bundledFormPersistsBothTypesAcrossActivityRestart() {
        assertTrue(InstrumentationRegistry.getInstrumentation().targetContext.packageName.endsWith(".offlinevalidation"))
        val owner="form-${System.currentTimeMillis()}"
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            val web=load(scenario,owner)
            eval(scenario,web,"document.getElementById('description').value='Synthetic expense';document.getElementById('amount').value='85,00';document.getElementById('date').value='2026-08-12';document.getElementById('expense').requestSubmit();document.getElementById('expense').requestSubmit();")
            waitFor(scenario,web,"document.querySelectorAll('#pending li').length","1")
            eval(scenario,web,"document.getElementById('kind').value='income';document.getElementById('kind').dispatchEvent(new Event('change'));document.getElementById('description').value='Synthetic income';document.getElementById('amount').value='2300,00';document.getElementById('date').value='2026-08-05';document.getElementById('expense').requestSubmit();")
            waitFor(scenario,web,"document.querySelectorAll('#pending li').length","2")
            scenario.onActivity { web.destroy() }
        }
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            val web=load(scenario,owner)
            waitFor(scenario,web,"document.querySelectorAll('#pending li').length","2")
            assertEquals("true",eval(scenario,web,"document.getElementById('pending').textContent.includes('Synthetic income')"))
            eval(scenario,web,"window.dispatchEvent(new CustomEvent('AndroidSecureSessionResult',{detail:{success:true,user_id:'other-$owner'}}))")
            waitFor(scenario,web,"document.getElementById('count').textContent.startsWith('0 ')","true")
            scenario.onActivity { web.destroy() }
        }
    }
}
