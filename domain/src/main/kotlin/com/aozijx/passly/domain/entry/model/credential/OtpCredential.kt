package com.aozijx.passly.domain.entry.model.credential

import com.aozijx.passly.domain.entry.model.otp.OtpConfig

data class OtpCredential(
    val config: OtpConfig
) : EntryCredential {
    override val kind: EntryCredentialKind = EntryCredentialKind.OTP
}
