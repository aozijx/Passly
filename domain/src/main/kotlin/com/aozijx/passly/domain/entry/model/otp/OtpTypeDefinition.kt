package com.aozijx.passly.domain.entry.model.otp

/** Parameters that belong to one algorithm family. */
sealed interface OtpFactorDefinition {
    val authUriKind: OtpAuthUriKind

    data class Time(
        val defaultPeriodSeconds: Int,
    ) : OtpFactorDefinition {
        override val authUriKind = OtpAuthUriKind.TIME_BASED

        init {
            require(defaultPeriodSeconds > 0)
        }
    }

    data class Counter(val initialValue: Long) : OtpFactorDefinition {
        override val authUriKind = OtpAuthUriKind.COUNTER_BASED

        init {
            require(initialValue >= 0L)
        }
    }
}

enum class OtpConfigParameter {
    ALGORITHM,
    DIGITS,
    PERIOD,
    COUNTER,
    ENCODING,
}

/** Domain contract for one OTP type. Each type owns its parameters and supported algorithms. */
sealed interface OtpTypeDefinition {
    val type: OtpType
    val key: String
    val defaultEntryTitle: String
    val factor: OtpFactorDefinition
    val allowedDigits: IntRange
    val defaultDigits: Int
    val defaultAlgorithm: OtpHashAlgorithm
    val supportedAlgorithms: Set<OtpHashAlgorithm>
    val editableParameters: Set<OtpConfigParameter>
    val issuerAlias: String?
}

data object TotpDefinition : OtpTypeDefinition {
    const val DEFAULT_PERIOD_SECONDS = 30
    const val DEFAULT_DIGITS = 6
    const val MIN_DIGITS = 6
    const val MAX_DIGITS = 8

    override val type = OtpType.TOTP
    override val key = "TOTP"
    override val defaultEntryTitle = "TOTP"
    override val factor = OtpFactorDefinition.Time(defaultPeriodSeconds = DEFAULT_PERIOD_SECONDS)
    override val allowedDigits = MIN_DIGITS..MAX_DIGITS
    override val defaultDigits = DEFAULT_DIGITS
    override val defaultAlgorithm = OtpHashAlgorithm.SHA1
    override val supportedAlgorithms = OtpHashAlgorithm.entries.toSet()
    override val editableParameters =
        setOf(OtpConfigParameter.ALGORITHM, OtpConfigParameter.DIGITS, OtpConfigParameter.PERIOD, OtpConfigParameter.ENCODING)
    override val issuerAlias: String? = null
}

data object HotpDefinition : OtpTypeDefinition {
    const val INITIAL_COUNTER = 0L
    const val DEFAULT_DIGITS = 6
    const val MIN_DIGITS = 6
    const val MAX_DIGITS = 8

    override val type = OtpType.HOTP
    override val key = "HOTP"
    override val defaultEntryTitle = "HOTP"
    override val factor = OtpFactorDefinition.Counter(initialValue = INITIAL_COUNTER)
    override val allowedDigits = MIN_DIGITS..MAX_DIGITS
    override val defaultDigits = DEFAULT_DIGITS
    override val defaultAlgorithm = OtpHashAlgorithm.SHA1
    override val supportedAlgorithms = OtpHashAlgorithm.entries.toSet()
    override val editableParameters =
        setOf(OtpConfigParameter.ALGORITHM, OtpConfigParameter.DIGITS, OtpConfigParameter.COUNTER, OtpConfigParameter.ENCODING)
    override val issuerAlias: String? = null
}

data object SteamDefinition : OtpTypeDefinition {
    const val PERIOD_SECONDS = 30
    const val DIGITS = 5

    override val type = OtpType.STEAM
    override val key = "STEAM"
    override val defaultEntryTitle = "Steam"
    override val factor = OtpFactorDefinition.Time(defaultPeriodSeconds = PERIOD_SECONDS)
    override val allowedDigits = DIGITS..DIGITS
    override val defaultDigits = DIGITS
    override val defaultAlgorithm = OtpHashAlgorithm.SHA1
    override val supportedAlgorithms = setOf(OtpHashAlgorithm.SHA1)
    override val editableParameters = setOf(OtpConfigParameter.ENCODING)
    override val issuerAlias = "Steam"
}
