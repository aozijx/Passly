package com.aozijx.passly.domain.entry.model.query

import com.aozijx.passly.domain.entry.model.EntrySecret

enum class EntryCapability {
    PASSWORD,
    OTP,
    SSH_KEY,
    PASSKEY,
    WIFI,
    IDENTITY,
    CARD,
    ATTACHMENTS,
    CUSTOM_FIELDS,
}

data class EntryCapabilities(
    val values: Set<EntryCapability> = emptySet(),
) {
    operator fun contains(capability: EntryCapability): Boolean = capability in values

    companion object {
        fun from(secret: EntrySecret, hasAttachments: Boolean = false): EntryCapabilities =
            EntryCapabilities(buildSet {
                if (
                    !secret.login?.password.isNullOrBlank() ||
                    !secret.wifi?.password.isNullOrBlank() ||
                    !secret.ssh?.passphrase.isNullOrBlank()
                ) add(EntryCapability.PASSWORD)
                if (!secret.otp?.config?.secret.isNullOrBlank()) add(EntryCapability.OTP)
                if (secret.ssh != null) add(EntryCapability.SSH_KEY)
                if (secret.passkey != null) add(EntryCapability.PASSKEY)
                if (secret.wifi != null) add(EntryCapability.WIFI)
                if (secret.identity != null) add(EntryCapability.IDENTITY)
                if (secret.card != null) add(EntryCapability.CARD)
                if (hasAttachments) add(EntryCapability.ATTACHMENTS)
                if (secret.customFields.isNotEmpty()) add(EntryCapability.CUSTOM_FIELDS)
            })
    }
}
