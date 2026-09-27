package com.gastointeligente.app

import org.junit.Assert.*
import org.junit.Test

class AndroidOAuthTest {
    private val attempt = OAuthAttempt("expected", 1000)
    private val callback = "https://gastointeligente.com.br/auth/android/callback?request=expected"
    private val tokens = "#access_token=fake-access&refresh_token=fake-refresh&expires_in=3600&token_type=bearer"

    @Test fun googleUsesExistingLovableBrokerAndFreshState() {
        val url = "https://gastointeligente.com.br/~oauth/initiate?provider=google&state=old&redirect_uri=https://example.org"
        val first = requireNotNull(AndroidOAuth.start(url, 1000))
        val second = requireNotNull(AndroidOAuth.start(url, 1000))
        assertNotEquals(first.attempt.state, second.attempt.state)
        assertEquals(64, first.attempt.state.length)
        assertFalse(first.browserUrl.contains("example.org"))
        assertTrue(first.browserUrl.contains("%2Fauth%2Fandroid%2Fcallback"))
    }

    @Test fun matchingReturnProducesOnlyTheExpectedTokenFields() {
        val result = requireNotNull(AndroidOAuth.accept(callback + tokens + "&unexpected=ignored", attempt, 1500))
        assertTrue(result.pageUrl!!.startsWith("https://gastointeligente.com.br/auth/android/callback#access_token="))
        assertFalse(result.pageUrl.contains("unexpected"))
        assertFalse(result.pageUrl.contains("request="))
    }

    @Test fun unsolicitedExpiredOrFutureAttemptsAreRejected() {
        assertNull(AndroidOAuth.accept(callback + tokens, null, 1500))
        assertNull(AndroidOAuth.accept(callback + tokens, attempt, 1000 + AndroidOAuth.MAX_AGE_MS + 1))
        assertNull(AndroidOAuth.accept(callback + tokens, attempt, 999))
    }

    @Test fun originPathPortAndRequestStateMustMatch() {
        for (bad in listOf(callback.replace("https:", "http:"), callback.replace(".com.br/", ".com.br.evil/"),
            callback.replace(".com.br/", ".com.br:444/"), callback.replace("/callback", "/callback/extra"),
            callback.replace("expected", "wrong"))) {
            assertNull(AndroidOAuth.accept(bad + tokens, attempt, 1500))
        }
    }

    @Test fun duplicatedParametersAndWrongBrokerStateAreRejected() {
        assertNull(AndroidOAuth.accept(callback + tokens + "&access_token=second", attempt, 1500))
        assertNull(AndroidOAuth.accept(callback + tokens + "&state=wrong", attempt, 1500))
        assertNull(AndroidOAuth.accept(callback + "&request=second" + tokens, attempt, 1500))
    }

    @Test fun errorReturnContainsNoProviderOrTokenDetails() {
        val result = requireNotNull(AndroidOAuth.accept(callback + "#error=access_denied&error_description=private", attempt, 1500))
        assertNull(result.pageUrl)
        assertFalse(result.error!!.contains("private"))
    }

    @Test fun invalidTokenPayloadIsRejected() {
        for (bad in listOf("#access_token=one", tokens.replace("expires_in=3600", "expires_in=-1"),
            tokens.replace("token_type=bearer", "token_type=other"), tokens.replace("fake-refresh", ""))) {
            assertNull(AndroidOAuth.accept(callback + bad, attempt, 1500))
        }
    }

    @Test fun unrelatedLinksDoNotStartOrCompleteAuthentication() {
        assertNull(AndroidOAuth.start("https://example.org/~oauth/initiate?provider=google", 1000))
        assertNull(AndroidOAuth.start("https://gastointeligente.com.br/~oauth/initiate?provider=apple", 1000))
        assertFalse(AndroidOAuth.isCallback("gastointeligente://auth/callback"))
    }
}
