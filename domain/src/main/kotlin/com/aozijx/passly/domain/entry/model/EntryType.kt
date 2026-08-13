package com.aozijx.passly.domain.entry.model

import com.aozijx.passly.domain.entry.model.credential.EntryCredentialKind

enum class EntryType(val credentialKind: EntryCredentialKind) {
    ACCOUNT(EntryCredentialKind.NONE),
    LOGIN(EntryCredentialKind.LOGIN),
    NOTE(EntryCredentialKind.NONE),
    BANK_CARD(EntryCredentialKind.CARD),
    ID_CARD(EntryCredentialKind.IDENTITY),
    PASSPORT(EntryCredentialKind.IDENTITY),
    DRIVER_LICENSE(EntryCredentialKind.IDENTITY),
    SSH_KEY(EntryCredentialKind.SSH),
    WIFI(EntryCredentialKind.WIFI),
    PASSKEY(EntryCredentialKind.PASSKEY),
    OTP(EntryCredentialKind.OTP),
    DATABASE_CREDENTIAL(EntryCredentialKind.LOGIN),
    SERVER_CREDENTIAL(EntryCredentialKind.LOGIN),
    API_KEY(EntryCredentialKind.LOGIN),
    CRYPTO_WALLET(EntryCredentialKind.LOGIN),
    SEED_PHRASE(EntryCredentialKind.IDENTITY),
    RECOVERY_CODE(EntryCredentialKind.IDENTITY),
}
