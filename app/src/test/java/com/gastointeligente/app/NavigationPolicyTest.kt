package com.gastointeligente.app

import org.junit.Assert.assertEquals
import org.junit.Test

class NavigationPolicyTest {
    @Test fun localServerIsInternalOnlyForDebugAndExactOrigin() {
        assertEquals(NavigationTarget.INTERNAL, navigationTarget("http://127.0.0.1:5173/app", debug = true))
        assertEquals(NavigationTarget.INTERNAL, navigationTarget("http://127.0.0.1:5173/gastos", debug = true))
        assertEquals(NavigationTarget.EXTERNAL, navigationTarget("http://127.0.0.1:5173/app", debug = false))
        for (url in listOf("http://127.0.0.1:5174/app", "http://localhost:5173/app", "http://127.0.0.1.evil.test:5173/app")) {
            assertEquals(NavigationTarget.EXTERNAL, navigationTarget(url, debug = true))
        }
        assertEquals(NavigationTarget.BLOCKED, navigationTarget("http://user@127.0.0.1:5173/app", debug = true))
    }
    @Test fun internalHttpsStaysInTheApp() {
        for (url in listOf("https://gastointeligente.com.br/app", "https://www.gastointeligente.com.br/gastos")) {
            assertEquals(NavigationTarget.INTERNAL, navigationTarget(url))
        }
    }
    @Test fun externalCheckoutRedirectOpensOutsideTheBridge() {
        assertEquals(NavigationTarget.EXTERNAL, navigationTarget("https://www.mercadopago.com.br/checkout", userGesture = false))
    }
    @Test fun externalFramesCannotLaunchApps() {
        assertEquals(NavigationTarget.BLOCKED, navigationTarget("https://example.com/", mainFrame = false))
    }
    @Test fun whatsappAndContactLinksRequireAnExplicitGesture() {
        for (url in listOf("whatsapp://send?text=Oi", "mailto:teste@example.com", "tel:+5511999999999")) {
            assertEquals(NavigationTarget.EXTERNAL, navigationTarget(url))
            assertEquals(NavigationTarget.BLOCKED, navigationTarget(url, userGesture = false))
        }
    }
    @Test fun unsafeUrlsCannotLoadInsideTheApp() {
        for (url in listOf("http://gastointeligente.com.br/app", "https://gastointeligente.com.br:444/app", "javascript:alert(1)", "file:///sdcard/test.html", "intent://send", "https://user:pass@gastointeligente.com.br/app", "not a url")) {
            assertEquals(url, NavigationTarget.BLOCKED, navigationTarget(url))
        }
    }
    @Test fun similarDomainIsNotTrusted() {
        assertEquals(NavigationTarget.EXTERNAL, navigationTarget("https://gastointeligente.com.br.evil.example/"))
    }
}
