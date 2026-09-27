package com.gastointeligente.app

import java.net.URI

/** Keeps a permission result bound to the page that requested it. */
internal class WebGeolocation(
    private val currentPage: () -> String?,
    private val hasPermission: () -> Boolean,
    private val requestPermission: () -> Unit,
) {
    private data class Pending(val origin: String, val reply: (Boolean) -> Unit)
    private var pending: Pending? = null
    private var requestingPermission = false

    private fun allowed(origin: String): Boolean {
        val page = currentPage() ?: return false
        if (navigationTarget(origin) != NavigationTarget.INTERNAL ||
            navigationTarget(page) != NavigationTarget.INTERNAL) return false
        val source = URI(origin)
        val target = URI(page)
        return source.host.equals(target.host, ignoreCase = true)
    }

    fun request(origin: String, reply: (Boolean) -> Unit) {
        if (!allowed(origin) || requestingPermission) {
            reply(false)
            return
        }
        if (hasPermission()) {
            reply(true)
            return
        }
        pending = Pending(origin, reply)
        requestingPermission = true
        try {
            requestPermission()
        } catch (_: Exception) {
            complete(false)
        }
    }

    fun complete(granted: Boolean) {
        requestingPermission = false
        val request = pending
        pending = null
        request?.reply?.invoke(granted && allowed(request.origin))
    }

    fun cancel() {
        val request = pending
        pending = null
        // A late Android result must not grant a newer page's request.
        request?.reply?.invoke(false)
    }
}
