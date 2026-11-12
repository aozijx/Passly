package com.aozijx.passly.domain.entry.model.otp

/** Complete Domain catalog; each type contributes its rules once. */
object OtpTypeDefinitions {
    private val definitions =
        buildOtpTypeDefinitionIndex(
            listOf(
                OtpTypeDefinition(
                    type = OtpType.TOTP,
                    key = "TOTP",
                    defaultEntryTitle = "TOTP",
                    movingFactor = OtpMovingFactor.TIME,
                    allowedDigits = 6..8,
                    defaultDigits = 6,
                    defaultPeriodSeconds = DEFAULT_PERIOD_SECONDS,
                    authUriKind = OtpAuthUriKind.TIME_BASED,
                ),
                OtpTypeDefinition(
                    type = OtpType.HOTP,
                    key = "HOTP",
                    defaultEntryTitle = "HOTP",
                    movingFactor = OtpMovingFactor.COUNTER,
                    allowedDigits = 6..8,
                    defaultDigits = 6,
                    defaultPeriodSeconds = null,
                    defaultCounter = 0L,
                    authUriKind = OtpAuthUriKind.COUNTER_BASED,
                ),
                OtpTypeDefinition(
                    type = OtpType.STEAM,
                    key = "STEAM",
                    defaultEntryTitle = "Steam",
                    movingFactor = OtpMovingFactor.TIME,
                    allowedDigits = 5..5,
                    defaultDigits = 5,
                    defaultPeriodSeconds = STEAM_PERIOD_SECONDS,
                    fixedPeriodSeconds = STEAM_PERIOD_SECONDS,
                    fixedAlgorithm = OtpHashAlgorithm.SHA1,
                    outputFormat = OtpOutputFormat.STEAM_ALPHABET,
                    authUriKind = OtpAuthUriKind.TIME_BASED,
                    issuerAlias = "Steam",
                ),
            ),
        )

    val all: Collection<OtpTypeDefinition> get() = definitions.values

    operator fun get(type: OtpType): OtpTypeDefinition = definitions.getValue(type)

    fun fromKey(key: String): OtpTypeDefinition? = definitions.values.firstOrNull { it.key == key }

    fun fromAuthUri(
        kind: OtpAuthUriKind,
        issuer: String?,
    ): OtpTypeDefinition? =
        definitions.values.firstOrNull { definition ->
            definition.authUriKind == kind &&
                definition.issuerAlias?.equals(issuer, ignoreCase = true) == true
        } ?: definitions.values.firstOrNull { definition ->
            definition.authUriKind == kind && definition.issuerAlias == null
        }

    private const val DEFAULT_PERIOD_SECONDS = 30
    private const val STEAM_PERIOD_SECONDS = 30
}

private fun buildOtpTypeDefinitionIndex(
    definitions: List<OtpTypeDefinition>,
): Map<OtpType, OtpTypeDefinition> {
    val duplicateTypes = definitions.groupingBy(OtpTypeDefinition::type).eachCount().filterValues { it > 1 }.keys
    require(duplicateTypes.isEmpty()) { "OTP type catalog contains duplicate registrations: $duplicateTypes" }

    val byType = definitions.associateBy(OtpTypeDefinition::type)
    val missingTypes = OtpType.entries.toSet() - byType.keys
    val unexpectedTypes = byType.keys - OtpType.entries.toSet()
    require(missingTypes.isEmpty() && unexpectedTypes.isEmpty()) {
        "OTP type catalog coverage mismatch; missing=$missingTypes, unexpected=$unexpectedTypes"
    }
    return byType
}
