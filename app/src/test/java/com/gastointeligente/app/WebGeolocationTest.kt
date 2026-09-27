package com.gastointeligente.app

import org.junit.Assert.*
import org.junit.Test

class WebGeolocationTest {
    private val origin = "https://gastointeligente.com.br"
    private var page: String? = "$origin/mercado"
    private var granted = false
    private var prompts = 0
    private val controller = WebGeolocation({ page }, { granted }, { prompts++ })

    @Test fun approximatePermissionAlreadyGrantedDoesNotPromptAgain() {
        granted = true
        val results = mutableListOf<Boolean>()
        controller.request(origin) { results.add(it) }
        assertEquals(listOf(true), results)
        assertEquals(0, prompts)
    }

    @Test fun deniedPermissionReturnsFailureToWebsite() {
        val results = mutableListOf<Boolean>()
        controller.request(origin) { results.add(it) }
        assertTrue(results.isEmpty())
        controller.complete(false)
        assertEquals(listOf(false), results)
        assertEquals(1, prompts)
    }

    @Test fun permissionGrantedCompletesOnlyOnce() {
        val results = mutableListOf<Boolean>()
        controller.request(origin) { results.add(it) }
        controller.complete(true)
        controller.complete(true)
        assertEquals(listOf(true), results)
    }

    @Test fun foreignInsecureAndSiblingOriginsAreRejectedWithoutPrompt() {
        for (source in listOf("https://example.org", "http://gastointeligente.com.br", "https://ads.gastointeligente.com.br")) {
            controller.request(source) { assertFalse(it) }
        }
        assertEquals(0, prompts)
    }

    @Test fun navigationBeforeAndroidResultDoesNotGrantAnotherPage() {
        val results = mutableListOf<Boolean>()
        controller.request(origin) { results.add(it) }
        page = "https://example.org"
        controller.complete(true)
        assertEquals(listOf(false), results)
    }

    @Test fun canceledRequestCannotBeReplacedUntilAndroidReturns() {
        val results = mutableListOf<Boolean>()
        controller.request(origin) { results.add(it) }
        controller.cancel()
        controller.request(origin) { results.add(it) }
        controller.complete(true)
        assertEquals(listOf(false, false), results)
        assertEquals(1, prompts)
        controller.request(origin) { results.add(it) }
        controller.complete(true)
        assertEquals(listOf(false, false, true), results)
    }

    @Test fun missingAndroidPermissionHandlerReturnsFailure() {
        val unavailable = WebGeolocation({ page }, { false }, { error("Unavailable") })
        unavailable.request(origin) { assertFalse(it) }
    }
}
