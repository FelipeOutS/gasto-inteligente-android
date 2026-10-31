package com.gastointeligente.app

import android.security.keystore.KeyPermanentlyInvalidatedException
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SessionFailurePolicyTest {
    @Test fun transientFailuresPreserveCredentials() {
        for (error in listOf(IllegalStateException("stale"), java.security.ProviderException("temporary"), javax.crypto.BadPaddingException(), NullPointerException())) {
            assertFalse(SessionFailurePolicy.invalidatedKey(error))
        }
    }
    @Test fun permanentlyInvalidatedKeyCanBeRemoved() {
        assertTrue(SessionFailurePolicy.invalidatedKey(KeyPermanentlyInvalidatedException()))
        assertTrue(SessionFailurePolicy.invalidatedKey(RuntimeException(KeyPermanentlyInvalidatedException())))
    }
}
