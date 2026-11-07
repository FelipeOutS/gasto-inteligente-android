package com.gastointeligente.app

import android.content.Context
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SystemNavigationTest {
    @Test fun optionalNavigationHidingPersistsAndCanBeDisabled() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        // This test must never reset a preference in the user's normal application.
        check(context.packageName.endsWith(".offlinevalidation"))
        val prefs = context.getSharedPreferences("system-navigation", Context.MODE_PRIVATE)
        val previous = prefs.all["hidden"] as? Boolean
        prefs.edit().remove("hidden").commit()
        try {
            ActivityScenario.launch(MainActivity::class.java).use { scenario ->
                fun awaitNavigation(visible: Boolean) {
                    var matched = false
                    repeat(60) {
                        scenario.onActivity { activity ->
                            val insets = ViewCompat.getRootWindowInsets(activity.window.decorView)
                            matched = insets != null && insets.isVisible(WindowInsetsCompat.Type.navigationBars()) == visible
                            if (matched) {
                                assertTrue("Status bar must remain visible", insets!!.isVisible(WindowInsetsCompat.Type.statusBars()))
                                assertTrue("Reserve space for transient system buttons", insets.getInsetsIgnoringVisibility(WindowInsetsCompat.Type.navigationBars()).bottom > 0)
                            }
                        }
                        if (matched) return
                        Thread.sleep(100)
                    }
                    fail("Navigation visibility did not become $visible")
                }
                assertFalse(prefs.getBoolean("hidden", false))
                awaitNavigation(true)
                prefs.edit().putBoolean("hidden", true).commit()
                scenario.onActivity { it.onWindowFocusChanged(true) }
                awaitNavigation(false)
                scenario.recreate()
                awaitNavigation(false)
                assertTrue(prefs.getBoolean("hidden", false))
                prefs.edit().putBoolean("hidden", false).commit()
                scenario.onActivity { it.onWindowFocusChanged(true) }
                awaitNavigation(true)
            }
        } finally {
            if (previous == null) prefs.edit().remove("hidden").commit()
            else prefs.edit().putBoolean("hidden", previous).commit()
        }
    }
}
