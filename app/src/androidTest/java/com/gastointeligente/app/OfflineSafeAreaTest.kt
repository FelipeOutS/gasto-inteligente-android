package com.gastointeligente.app

import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/** Checks real Android insets on the bundled offline page, without unlocking or writing records. */
@RunWith(AndroidJUnit4::class)
class OfflineSafeAreaTest {
    private fun find(view: View): WebView? {
        if (view is WebView) return view
        if (view is ViewGroup) for (i in 0 until view.childCount) find(view.getChildAt(i))?.let { return it }
        return null
    }
    @Test fun portraitInsets() = verifyInsets(android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
    @Test fun landscapeInsets() = verifyInsets(android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)
    private fun verifyInsets(orientation: Int) {
        assertTrue(InstrumentationRegistry.getInstrumentation().targetContext.packageName.endsWith(".offlinevalidation"))
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { it.requestedOrientation = orientation }
            var result = ""
            val deadline = android.os.SystemClock.elapsedRealtime() + 15000
            while (android.os.SystemClock.elapsedRealtime() < deadline) {
                val latch = CountDownLatch(1)
                scenario.onActivity { activity ->
                    val web = requireNotNull(find(activity.findViewById(android.R.id.content)))
                    web.evaluateJavascript("""(()=>{
                        const root=getComputedStyle(document.documentElement);
                        const top=parseFloat(root.getPropertyValue('--android-safe-top'));
                        const bottom=parseFloat(root.getPropertyValue('--android-safe-bottom'));
                        const left=parseFloat(root.getPropertyValue('--android-safe-left')) || 0; const right=parseFloat(root.getPropertyValue('--android-safe-right')) || 0; const logo=document.querySelector('.logo');
                        const back=document.querySelector('.back');
                        const unlock=document.getElementById('unlock');
                        const initial = top>0 && bottom>=0 && !!logo && logo.complete && logo.naturalWidth>0 &&
                            logo.getBoundingClientRect().top>=top+12 &&
                            logo.getBoundingClientRect().left>=left && unlock.getBoundingClientRect().right<=innerWidth-right &&
                            back.getBoundingClientRect().top>=top+12 &&
                            unlock.getBoundingClientRect().top>=top+12 &&
                            parseFloat(getComputedStyle(document.querySelector('main')).paddingBottom)>=bottom &&
                            document.documentElement.scrollWidth<=innerWidth;
                        if (!initial) return false;
                        const main=document.querySelector('main');
                        const content=document.getElementById('content');
                        // Layout-only exposure in the isolated package. No unlock, bridge call or save.
                        content.hidden=false;
                        let safe=true;
                        for(const offset of [200,600,main.scrollHeight]) {
                            main.scrollTop=offset;
                            const rect=main.getBoundingClientRect();
                            safe=safe && main.scrollTop>0 && rect.top>=top && rect.bottom<=innerHeight-bottom+1;
                            for(const y of [Math.max(0,top-1),innerHeight-1]) {
                                const hit=document.elementFromPoint(innerWidth/2,y);
                                safe=safe && !(hit && main.contains(hit));
                            }
                        }
                        main.scrollTop=main.scrollHeight;
                        const end=document.querySelector('.privacy').getBoundingClientRect();
                        safe=safe && end.bottom<=innerHeight-bottom+1 && window.scrollY===0;
                        content.hidden=true;
                        main.scrollTop=0;
                        return safe;
                    })()""") { result = it; latch.countDown() }
                }
                assertTrue(latch.await(5, TimeUnit.SECONDS))
                val targetLandscape = orientation == android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                val actualLandscape = InstrumentationRegistry.getInstrumentation().targetContext.resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
                if (result == "true" && targetLandscape == actualLandscape) break
                android.os.SystemClock.sleep(150)
            }
            assertEquals("Logo, link and unlock must respect native system insets", "true", result)
        }
    }
}
