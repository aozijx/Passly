package com.aozijx.passly.domain.entry.model.otp

/** Identity and URI routing shared by separately owned OTP definitions. */
sealed interface OtpTypeDefinition {
    val type: OtpType
    val key: String
    val defaultEntryTitle: String
    val authUriKind: OtpAuthUriKind
    val issuerAlias: String?
}
