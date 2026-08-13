package com.aozijx.passly.domain.entry.service

import com.aozijx.passly.domain.entry.model.EntrySecret
import com.aozijx.passly.domain.entry.model.EntryType

/**
 * Enforces one primary credential payload per Entry.
 *
 * Notes and custom fields are common extensions. All typed payload slots other
 * than the one selected by [EntryType] must remain empty.
 */
object EntrySecretPolicy {

    fun requireValid(type: EntryType, secret: EntrySecret) {
        val populated = buildSet {
            if (secret.login != null) add(PayloadKind.LOGIN)
            if (secret.card != null) add(PayloadKind.CARD)
            if (secret.identity != null) add(PayloadKind.IDENTITY)
            if (secret.ssh != null) add(PayloadKind.SSH)
            if (secret.wifi != null) add(PayloadKind.WIFI)
            if (secret.passkey != null) add(PayloadKind.PASSKEY)
            if (secret.otp != null) add(PayloadKind.OTP)
        }
        val allowed = type.allowedPayloadKind()
        require(populated.size <= 1) {
            "Entry $type contains multiple credential payloads: $populated"
        }
        require(populated.isEmpty() || populated.single() == allowed) {
            "Entry $type contains incompatible credential payload: $populated"
        }
        if (type == EntryType.ACCOUNT) {
            require(secret == EntrySecret()) {
                "ACCOUNT entries may only contain summary metadata"
            }
        }
    }
}

private enum class PayloadKind {
    LOGIN,
    CARD,
    IDENTITY,
    SSH,
    WIFI,
    PASSKEY,
    OTP
}

private fun EntryType.allowedPayloadKind(): PayloadKind? = when (this) {
    EntryType.ACCOUNT, EntryType.NOTE -> null
    EntryType.CARD, EntryType.BANK_CARD -> PayloadKind.CARD
    EntryType.IDENTITY,
    EntryType.PASSPORT,
    EntryType.LICENSE,
    EntryType.ID_CARD,
    EntryType.SEED_PHRASE,
    EntryType.RECOVERY_CODE -> PayloadKind.IDENTITY

    EntryType.SSH_KEY -> PayloadKind.SSH
    EntryType.WIFI -> PayloadKind.WIFI
    EntryType.PASSKEY -> PayloadKind.PASSKEY
    EntryType.OTP -> PayloadKind.OTP
    EntryType.LOGIN,
    EntryType.DATABASE,
    EntryType.SERVER,
    EntryType.API_KEY,
    EntryType.CRYPTO_WALLET -> PayloadKind.LOGIN
}
