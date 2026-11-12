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

    fun configuration(
        secret: String? = null,
        encoding: OtpSecretEncoding = OtpSecretEncoding.BASE32,
        issuer: String? = null,
        accountName: String? = null,
    ): OtpConfig.Totp =
        OtpConfig.Totp(
            secret = secret,
            algorithm = defaultAlgorithm,
            digits = DIGITS,
            periodSeconds = PERIOD_SECONDS,
            encoding = encoding,
            issuer = issuer,
            accountName = accountName,
            variant = TotpVariant.STEAM,
        )
}
