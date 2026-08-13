package com.aozijx.passly.domain.entry.model

/** Aggregate root for a single independently stored entry. */
data class Entry(
    val identity: EntryIdentity,
    val profile: EntryProfile,
    val secret: EntrySecret = EntrySecret(),
) {
    init {
        require(secret.credential.kind == identity.type.credentialKind) {
            "${identity.type} cannot contain ${secret.credential.kind} credentials"
        }
        require(identity.type != EntryType.ACCOUNT || secret.isEmpty) {
            "Account grouping entries cannot contain secret data"
        }
    }

    val id: EntryId get() = identity.id
    val type: EntryType get() = identity.type
    val version: EntryVersion get() = identity.version
    val timestamps: EntryTimestamps get() = identity.timestamps
}
