package com.aozijx.passly.domain.settings.model

/** Immutable snapshot of the policy inputs used by the OTP runtime. */
data class OtpRuntimePolicy(
    val copyNextTotpNearExpiry: Boolean = true,
)
