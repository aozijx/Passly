package com.aozijx.passly.feature.autofill.shared

import android.os.SystemClock
import com.aozijx.passly.domain.autofill.model.AutofillGrantContext
import com.aozijx.passly.domain.autofill.port.AutofillGrantStore

/**
 * Short-lived autofill grant store (single slot + TTL).
 *
 * Used to avoid redundant biometric prompts during a multi-step autofill flow.
 */
class AutofillSessionGrantStore(
    private val ttlMillis: Long = DEFAULT_TTL_MILLIS,
    private val elapsedRealtime: () -> Long = SystemClock::elapsedRealtime,
) : AutofillGrantStore {
    private data class Grant(
        val context: AutofillGrantContext,
        val expiresAtMillis: Long,
    )

    private var activeGrant: Grant? = null

    @Synchronized
    override fun grant(context: AutofillGrantContext) {
        val normalized = context.normalized()
        if (normalized.packageName.isBlank()) return
        activeGrant = Grant(
            context = normalized,
            expiresAtMillis = elapsedRealtime() + ttlMillis,
        )
    }

    @Synchronized
    override fun isGranted(context: AutofillGrantContext): Boolean {
        val grant = activeGrant ?: return false
        if (elapsedRealtime() >= grant.expiresAtMillis) {
            activeGrant = null
            return false
        }
        val normalized = context.normalized()
        return grant.context.packageName == normalized.packageName &&
            grant.context.webDomain == normalized.webDomain
    }

    @Synchronized
    override fun clear() {
        activeGrant = null
    }

    companion object {
        const val DEFAULT_TTL_MILLIS = 30_000L
    }
}
