package com.aozijx.passly.feature.detail.components

import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.model.VaultEntry

enum class DetailSectionKey {
    CREDENTIAL,
    OTP,
    BANK_CARD,
    IDENTITY,
    WIFI,
    SSH,
    SEED_PHRASE,
    PASSKEY,
    ENTRY_TYPE,
    ASSOCIATED_INFO,
    NOTES,
    METADATA,
    ACTIVITY,
}

/**
 * Resolves the sections shown by a single entry's detail page.
 *
 * The entry type supplies the base layout. Structured secret capabilities can
 * add sections to a mixed entry, such as a login that also owns an OTP or a
 * passkey. This is deliberately separate from the vault card-style registry.
 */
object DetailSectionResolver {
    private val typeSections: Map<EntryType, Set<DetailSectionKey>> = mapOf(
        EntryType.LOGIN to setOf(DetailSectionKey.CREDENTIAL),
        EntryType.NOTE to setOf(DetailSectionKey.CREDENTIAL),
        EntryType.OTP to setOf(DetailSectionKey.OTP),
        EntryType.CARD to setOf(DetailSectionKey.BANK_CARD),
        EntryType.BANK_CARD to setOf(DetailSectionKey.BANK_CARD),
        EntryType.IDENTITY to setOf(DetailSectionKey.IDENTITY),
        EntryType.ID_CARD to setOf(DetailSectionKey.IDENTITY),
        EntryType.PASSPORT to setOf(DetailSectionKey.IDENTITY),
        EntryType.LICENSE to setOf(DetailSectionKey.IDENTITY),
        EntryType.SSH_KEY to setOf(DetailSectionKey.SSH),
        EntryType.WIFI to setOf(DetailSectionKey.WIFI),
        EntryType.SEED_PHRASE to setOf(DetailSectionKey.SEED_PHRASE),
        EntryType.PASSKEY to setOf(DetailSectionKey.PASSKEY),
        EntryType.RECOVERY_CODE to setOf(DetailSectionKey.CREDENTIAL),
        EntryType.DATABASE to setOf(DetailSectionKey.CREDENTIAL),
        EntryType.SERVER to setOf(DetailSectionKey.CREDENTIAL),
        EntryType.API_KEY to setOf(DetailSectionKey.CREDENTIAL),
        EntryType.CRYPTO_WALLET to setOf(DetailSectionKey.CREDENTIAL),
    )

    private val commonSections = listOf(
        DetailSectionKey.ENTRY_TYPE,
        DetailSectionKey.ASSOCIATED_INFO,
        DetailSectionKey.NOTES,
        DetailSectionKey.METADATA,
        DetailSectionKey.ACTIVITY,
    )

    fun resolve(entry: VaultEntry): List<DetailSectionKey> {
        val selected = typeSections[entry.entryType].orEmpty().toMutableSet()
        with(entry.secret) {
            if (login != null) selected += DetailSectionKey.CREDENTIAL
            if (otp != null) selected += DetailSectionKey.OTP
            if (card != null) selected += DetailSectionKey.BANK_CARD
            if (wifi != null) selected += DetailSectionKey.WIFI
            if (ssh != null) selected += DetailSectionKey.SSH
            if (passkey != null) selected += DetailSectionKey.PASSKEY
            if (!identity?.seedPhrase.isNullOrBlank()) {
                selected += DetailSectionKey.SEED_PHRASE
            }
            if (
                !identity?.idNumber.isNullOrBlank() ||
                !identity?.securityQuestion.isNullOrBlank() ||
                !identity?.securityAnswer.isNullOrBlank()
            ) {
                selected += DetailSectionKey.IDENTITY
            }
        }
        return DetailSectionKey.entries.filter(selected::contains) + commonSections
    }
}
