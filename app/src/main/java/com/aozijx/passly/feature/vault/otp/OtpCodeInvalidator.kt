package com.aozijx.passly.feature.vault.otp

internal interface OtpCodeInvalidator {
    fun entryChanged(entryId: String)

    fun entryRemoved(entryId: String)
}
