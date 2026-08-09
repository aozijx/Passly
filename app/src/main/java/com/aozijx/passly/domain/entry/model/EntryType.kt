package com.aozijx.passly.domain.entry.model

enum class EntryType {
    ACCOUNT,
    LOGIN,
    NOTE,
    CARD,
    IDENTITY,
    SSH_KEY,
    WIFI,
    PASSKEY,
    OTP,
    PASSPORT,
    LICENSE,
    DATABASE,
    SERVER,
    API_KEY,
    CRYPTO_WALLET,
    BANK_CARD,
    ID_CARD,
    SEED_PHRASE,
    RECOVERY_CODE;

    companion object {
        fun fromName(name: String): EntryType =
            entries.find { it.name == name }
                ?: throw IllegalArgumentException("Unknown EntryType: $name")
    }
}
