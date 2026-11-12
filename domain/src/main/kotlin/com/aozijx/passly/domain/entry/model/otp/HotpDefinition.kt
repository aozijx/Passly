package com.aozijx.passly.domain.entry.model.otp

data object HotpDefinition : OtpTypeDefinition {
    const val INITIAL_COUNTER = 0L
    const val DEFAULT_DIGITS = 6
    const val MIN_DIGITS = 6
    const val MAX_DIGITS = 8

    override val type = OtpType.HOTP
    override val key = "HOTP"
    override val defaultEntryTitle = "HOTP"
    override val authUriKind = OtpAuthUriKind.COUNTER_BASED
    override val issuerAlias: String? = null

    val allowedDigits = MIN_DIGITS..MAX_DIGITS
    val defaultAlgorithm = OtpHashAlgorithm.SHA1
    val supportedAlgorithms = OtpHashAlgorithm.entries.toSet()
}
