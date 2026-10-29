package com.aozijx.passly.data.codec.entry.payload

import kotlinx.serialization.Serializable

/**
 * 原子凭据载荷。
 *
 * [EntryEntity.entryType] is the discriminator. Repository policy permits at
 * most one typed payload slot; notes and custom fields are common extensions.
 * The reset development schema starts again at payload version 1.
 */
@Serializable
data class SecretPayload(
    val login: LoginCredentialPayload? = null,
    val notes: String? = null,
    val card: CardCredentialPayload? = null,
    val identity: IdentityCredentialPayload? = null,
    val ssh: SshCredentialPayload? = null,
    val wifi: WifiCredentialPayload? = null,
    val passkey: PasskeyCredentialPayload? = null,
    val otp: OtpCredentialPayload? = null,
    val customFields: List<CustomFieldPayload> = emptyList(),
    val schemaVersion: Int = 1,
)
