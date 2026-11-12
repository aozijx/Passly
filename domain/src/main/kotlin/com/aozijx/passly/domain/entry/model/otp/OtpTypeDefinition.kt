package com.aozijx.passly.domain.entry.model.otp

/** Domain rules for one built-in OTP type, independent of labels and platform UI. */
data class OtpTypeDefinition(
    val type: OtpType,
    val key: String,
    val defaultEntryTitle: String,
    val movingFactor: OtpMovingFactor,
    val allowedDigits: IntRange,
    val defaultDigits: Int,
    val defaultPeriodSeconds: Int?,
    val fixedPeriodSeconds: Int? = null,
    val defaultCounter: Long? = null,
    val fixedAlgorithm: OtpHashAlgorithm? = null,
    val outputFormat: OtpOutputFormat = OtpOutputFormat.DECIMAL,
    val authUriKind: OtpAuthUriKind,
    val issuerAlias: String? = null,
) {
    init {
        require(key.isNotBlank())
        require(defaultEntryTitle.isNotBlank())
        require(!allowedDigits.isEmpty() && defaultDigits in allowedDigits)
        require((movingFactor == OtpMovingFactor.TIME) == (defaultPeriodSeconds != null))
        require((movingFactor == OtpMovingFactor.COUNTER) == (defaultCounter != null))
        require((movingFactor == OtpMovingFactor.TIME) == (authUriKind == OtpAuthUriKind.TIME_BASED))
        require(fixedPeriodSeconds == null || fixedPeriodSeconds > 0)
        require(fixedPeriodSeconds == null || movingFactor == OtpMovingFactor.TIME)
        require(issuerAlias == null || issuerAlias.isNotBlank())
    }
}
