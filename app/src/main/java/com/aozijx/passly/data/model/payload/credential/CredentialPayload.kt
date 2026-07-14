package com.aozijx.passly.data.model.payload.credential

import kotlinx.serialization.Serializable

@Serializable
data class CredentialPayload(
    val login: LoginPayload? = null,

    val totp: TotpPayload? = null,
    val passkey: PasskeyPayload? = null,

    val card: CardPayload? = null,
    val identity: IdentityPayload? = null,
    val wifi: WifiPayload? = null,
    val securityQuestion: SecurityQuestionPayload? = null,
    val payment: PaymentPayload? = null,

    val ssh: SshPayload? = null,

    val customFields: List<CustomFieldPayload> = emptyList()
) {
    companion object {
        val EMPTY = CredentialPayload()
    }
}