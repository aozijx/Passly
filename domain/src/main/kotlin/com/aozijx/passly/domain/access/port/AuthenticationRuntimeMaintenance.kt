package com.aozijx.passly.domain.access.port

/** Security-owned startup reconciliation exposed to the application composition root. */
fun interface AuthenticationRuntimeMaintenance {
    suspend fun reconcileStartupState()
}