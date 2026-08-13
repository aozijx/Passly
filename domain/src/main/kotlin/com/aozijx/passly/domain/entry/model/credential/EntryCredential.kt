package com.aozijx.passly.domain.entry.model.credential

enum class EntryCredentialKind { NONE, LOGIN, CARD, IDENTITY, SSH, WIFI, PASSKEY, OTP }

sealed interface EntryCredential {
    val kind: EntryCredentialKind

    data object None : EntryCredential {
        override val kind: EntryCredentialKind = EntryCredentialKind.NONE
    }
}
