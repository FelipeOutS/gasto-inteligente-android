package com.gastointeligente.app

import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/** Real WebView/IndexedDB, synthetic writer only. No production API calls. */
@RunWith(AndroidJUnit4::class)
class OfflineConnectedAccountTest {
    private fun find(view: View): WebView? {
        if (view is WebView) return view
        if (view is ViewGroup) for (i in 0 until view.childCount) find(view.getChildAt(i))?.let { return it }
        return null
    }
    private fun eval(scenario: ActivityScenario<MainActivity>, web: WebView, script: String): String {
        val latch = CountDownLatch(1)
        var value = ""
        scenario.onActivity { web.evaluateJavascript(script) { value = it; latch.countDown() } }
        assertTrue(latch.await(5,TimeUnit.SECONDS))
        return value
    }
    private fun waitFor(scenario: ActivityScenario<MainActivity>, web: WebView, expression: String, expected: String) {
        val deadline=android.os.SystemClock.elapsedRealtime()+10000
        var actual=""
        while(android.os.SystemClock.elapsedRealtime()<deadline) {
            actual=eval(scenario,web,expression)
            if(actual==expected)return
            android.os.SystemClock.sleep(100)
        }
        assertEquals(expression,expected,actual)
    }
    private fun load(scenario: ActivityScenario<MainActivity>): WebView {
        val script=InstrumentationRegistry.getInstrumentation().context.assets.open("offline-accounts-fixture.js").bufferedReader().use { it.readText() }
        lateinit var web: WebView
        scenario.onActivity { activity ->
            web=requireNotNull(find(activity.findViewById(android.R.id.content)))
            web.stopLoading()
            web.webViewClient=WebViewClient()
            web.settings.blockNetworkLoads=true
            web.loadDataWithBaseURL("https://offline-p1.example.invalid", "<html><script>$script</script></html>", "text/html", "UTF-8", null)
        }
        waitFor(scenario,web,"typeof window.incomes","\"object\"")
        return web
    }
    @Test fun connectedQueueSurvivesActivityCloseAndSendsToOriginalOwner() {
        val actor="actor-${System.currentTimeMillis()}"
        val owner="connected-$actor"
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            val web=load(scenario)
            eval(scenario,web,"(async()=>{await incomes.enqueueIncome('$owner',{descricao:'Receita conectada',valor:50,data:'2026-09-21',tipo:'outros'},'$actor');await expenses.enqueueExpense('$owner',{descricao:'Gasto conectado',valor:10,data:'2026-09-21',formaPagamento:'pix',categoriaId:'outros'},'$actor');window.saved=true;})()")
            waitFor(scenario,web,"window.saved","true")
        }
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            val web=load(scenario)
            eval(scenario,web,"(async()=>{window.activeOwner='$actor';await Promise.all([syncAllForUser('$actor'),syncAllIncomesForUser('$actor')]);window.finished=true;})()")
            waitFor(scenario,web,"window.finished","true")
            assertEquals("2",eval(scenario,web,"writes.length"))
            assertEquals("true",eval(scenario,web,"writes.every(x=>x.owner==='$owner' && x.actor==='$actor')"))
            eval(scenario,web,"(async()=>{window.remaining=(await incomes.listIncomes('$actor','$actor',true)).length+(await expenses.listExpenses('$actor','$actor',true)).length;})()")
            waitFor(scenario,web,"window.remaining","0")
        }
    }
}
