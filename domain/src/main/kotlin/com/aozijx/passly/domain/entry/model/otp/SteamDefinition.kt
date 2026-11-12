package com.aozijx.passly.domain.entry.model.otp

data object SteamDefinition : OtpTypeDefinition {
    const val PERIOD_SECONDS = 30
    const val DIGITS = 5

    override val type = OtpType.STEAM
    override val key = "STEAM"
    override val defaultEntryTitle = "Steam"
    override val authUriKind = OtpAuthUriKind.TIME_BASED
    override val issuerAlias = "Steam"

    val defaultAlgorithm = OtpHashAlgorithm.SHA1
    val supportedAlgorithms = setOf(OtpHashAlgorithm.SHA1)
}
