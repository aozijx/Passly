package com.aozijx.passly.feature.vault.components.editor

import com.aozijx.passly.domain.entry.model.EntryHeader
import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.entry.model.EntrySecret
import com.aozijx.passly.domain.entry.model.EntrySummary
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.model.EntryVersion
import com.aozijx.passly.domain.entry.model.VaultEntry
import com.aozijx.passly.domain.entry.model.secret.CardSecret
import com.aozijx.passly.domain.entry.model.secret.IdentitySecret
import com.aozijx.passly.domain.entry.model.secret.LoginSecret
import com.aozijx.passly.domain.entry.model.secret.PasskeySecret
import com.aozijx.passly.domain.entry.model.secret.SshSecret
import com.aozijx.passly.domain.entry.model.secret.WifiSecret

fun EntryEditorSchema.toVaultEntry(state: EntryEditorFormState): VaultEntry {
    val now = System.currentTimeMillis()
    return VaultEntry(
        header = EntryHeader(
            id = EntryId(""),
            entryType = entryType,
            version = EntryVersion.INITIAL,
            createdAt = now,
            updatedAt = now
        ),
        summary = EntrySummary(
            title = state.value(EntryEditorFieldKey.TITLE),
            username = state.value(EntryEditorFieldKey.SUMMARY),
            tags = state.value(EntryEditorFieldKey.TAGS).toSummaryTags(),
            icon = null
        ),
        secret = secretFor(
            type = entryType,
            value = state.value(EntryEditorFieldKey.SECRET),
            notes = state.value(EntryEditorFieldKey.NOTES).ifBlank { null }
        )
    )
}

private fun secretFor(type: EntryType, value: String, notes: String?): EntrySecret = when (type) {
    EntryType.BANK_CARD, EntryType.CARD ->
        EntrySecret(card = CardSecret(cardNumber = value), notes = notes)

    EntryType.WIFI ->
        EntrySecret(wifi = WifiSecret(password = value), notes = notes)

    EntryType.SSH_KEY ->
        EntrySecret(ssh = SshSecret(privateKey = value), notes = notes)

    EntryType.ID_CARD, EntryType.IDENTITY, EntryType.PASSPORT, EntryType.LICENSE ->
        EntrySecret(identity = IdentitySecret(idNumber = value), notes = notes)

    EntryType.SEED_PHRASE ->
        EntrySecret(identity = IdentitySecret(seedPhrase = value), notes = notes)

    EntryType.RECOVERY_CODE ->
        EntrySecret(
            identity = IdentitySecret(
                recoveryCodes = value.lines().map(String::trim).filter(String::isNotEmpty)
            ),
            notes = notes
        )

    EntryType.PASSKEY ->
        EntrySecret(passkey = PasskeySecret(privateKeyReference = value), notes = notes)

    else -> EntrySecret(login = LoginSecret(password = value), notes = notes)
}

private fun String.toSummaryTags(): List<String> =
    split(',', '，', ';', '；', '\n')
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .distinctBy { it.lowercase() }
