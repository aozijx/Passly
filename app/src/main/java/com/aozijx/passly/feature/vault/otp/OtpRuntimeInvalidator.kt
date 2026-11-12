package com.aozijx.passly.feature.vault.otp

/** Allows a committed vault replacement to invalidate OTP material held by active leases. */
internal fun interface OtpRuntimeInvalidator {
    suspend fun vaultReplaced()
}
