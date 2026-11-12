package com.aozijx.passly.feature.vault.otp

import kotlinx.coroutines.flow.StateFlow

/** Vault operations that page owners need from the app-scoped OTP runtime. */
internal interface OtpCodeRuntimePort {
    val states: StateFlow<Map<String, OtpCodeState>>

    fun acquire(entryId: String): OtpLease

    suspend fun codeForCopy(entryId: String): String?

    suspend fun rollbackLastHotpCounter(entryId: String): Boolean

    fun entryRemoved(entryId: String)

    fun entryChanged(entryId: String)
}
