package com.gastointeligente.app

import android.security.keystore.KeyPermanentlyInvalidatedException

/** Transient provider errors and stale operations must not erase a saved credential. */
internal object SessionFailurePolicy {
    fun invalidatedKey(error: Throwable): Boolean {
        var current: Throwable? = error
        repeat(8) {
            if (current is KeyPermanentlyInvalidatedException) return true
            current = current?.cause
        }
        return false
    }
}
