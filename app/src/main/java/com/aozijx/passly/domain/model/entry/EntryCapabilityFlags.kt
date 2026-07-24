package com.aozijx.passly.domain.model.entry

object EntryCapabilityFlags {
    const val HAS_PASSWORD = 1 shl 0
    const val HAS_OTP = 1 shl 1
    const val HAS_SSH_KEY = 1 shl 2
    const val HAS_PASSKEY = 1 shl 3
    const val HAS_WIFI = 1 shl 4
    const val HAS_IDENTITY = 1 shl 5
    const val HAS_CARD = 1 shl 6
    const val HAS_ATTACHMENTS = 1 shl 7
    const val HAS_CUSTOM_FIELDS = 1 shl 8

    fun computeFrom(secret: EntrySecret): Int {
        var flags = 0
        if (secret.login?.password?.isNotEmpty() == true) flags = flags or HAS_PASSWORD
        if (secret.otp?.config?.secret?.isNotBlank() == true) flags = flags or HAS_OTP
        if (secret.ssh != null) flags = flags or HAS_SSH_KEY
        if (secret.passkey != null) flags = flags or HAS_PASSKEY
        if (secret.wifi != null) flags = flags or HAS_WIFI
        if (secret.identity != null) flags = flags or HAS_IDENTITY
        if (secret.card != null) flags = flags or HAS_CARD
        if (secret.customFields.isNotEmpty()) flags = flags or HAS_CUSTOM_FIELDS
        return flags
    }

    fun otpTypeFrom(secret: EntrySecret): String? =
        secret.otp?.config?.type?.name
}
