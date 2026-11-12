package com.aozijx.passly.domain.entry.model.otp

/** Built-in definition registry. A new OTP type adds its own definition and config variant. */
object OtpTypeDefinitions {
    private val definitions =
        listOf(TotpDefinition, HotpDefinition, SteamDefinition)
            .also(::validateDefinitions)
            .associateBy(OtpTypeDefinition::type)

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
}

private fun validateDefinitions(definitions: List<OtpTypeDefinition>) {
    val duplicates =
        definitions
            .groupingBy(OtpTypeDefinition::type)
            .eachCount()
            .filterValues { it > 1 }
            .keys
    require(duplicates.isEmpty()) { "OTP type definitions must be unique: $duplicates" }
    require(definitions.all { it.key.isNotBlank() && it.defaultEntryTitle.isNotBlank() }) {
        "OTP type identity is invalid"
    }

    val missing = OtpType.entries.toSet() - definitions.map(OtpTypeDefinition::type).toSet()
    require(missing.isEmpty()) { "OTP type definitions are missing: $missing" }
}
