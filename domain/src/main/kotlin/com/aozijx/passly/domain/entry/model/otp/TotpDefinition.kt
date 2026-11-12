package com.aozijx.passly.domain.entry.model.otp

data object TotpDefinition : OtpTypeDefinition {
    const val DEFAULT_PERIOD_SECONDS = 30
    const val DEFAULT_DIGITS = 6
    const val MIN_DIGITS = 6
    const val MAX_DIGITS = 8

    override val type = OtpType.TOTP
    override val key = "TOTP"
    override val defaultEntryTitle = "TOTP"
    override val authUriKind = OtpAuthUriKind.TIME_BASED
    override val issuerAlias: String? = null

    val allowedDigits = MIN_DIGITS..MAX_DIGITS
    val defaultAlgorithm = OtpHashAlgorithm.SHA1
    val supportedAlgorithms = OtpHashAlgorithm.entries.toSet()
}
