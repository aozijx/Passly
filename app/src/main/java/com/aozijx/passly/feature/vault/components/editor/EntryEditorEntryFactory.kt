package com.aozijx.passly.feature.vault.components.editor

import com.aozijx.passly.domain.entry.model.EntryIdentity
import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.entry.model.EntrySecret
import com.aozijx.passly.domain.entry.model.EntryProfile
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.model.EntryVersion
import com.aozijx.passly.domain.entry.model.EntryTimestamps
import com.aozijx.passly.domain.entry.model.EntryIcon
import com.aozijx.passly.domain.entry.model.Entry
import com.aozijx.passly.domain.entry.model.credential.CardCredential
import com.aozijx.passly.domain.entry.model.credential.IdentityCredential
import com.aozijx.passly.domain.entry.model.credential.LoginCredential
import com.aozijx.passly.domain.entry.model.credential.PasskeyCredential
import com.aozijx.passly.domain.entry.model.credential.SshCredential
import com.aozijx.passly.domain.entry.model.credential.WifiCredential
import com.aozijx.passly.domain.entry.model.credential.OtpCredential
import com.aozijx.passly.domain.entry.model.credential.EntryCredential
import com.aozijx.passly.domain.entry.model.otp.OtpConfig
import com.github.f4b6a3.uuid.UuidCreator

fun EntryEditorSchema.toEntry(state: EntryEditorFormState): Entry {
    val now = System.currentTimeMillis()
    return Entry(
        identity = EntryIdentity(
            id = EntryId(UuidCreator.getTimeOrderedEpoch().toString()),
            type = entryType,
            version = EntryVersion.INITIAL,
            timestamps = EntryTimestamps(now),
        ),
        profile = EntryProfile(
            title = state.value(EntryEditorFieldKey.TITLE),
            username = state.value(EntryEditorFieldKey.SUMMARY),
            tags = state.value(EntryEditorFieldKey.TAGS).toSummaryTags(),
            icon = EntryIcon(),
        ),
        secret = secretFor(
            type = entryType,
            value = state.value(EntryEditorFieldKey.SECRET),
            notes = state.value(EntryEditorFieldKey.NOTES).ifBlank { null }
        )
    )
}

private fun secretFor(type: EntryType, value: String, notes: String?): EntrySecret = when (type) {
    EntryType.BANK_CARD ->
        EntrySecret(credential = CardCredential(cardNumber = value), notes = notes)

    EntryType.WIFI ->
        EntrySecret(credential = WifiCredential(ssid = value, password = value), notes = notes)

    EntryType.SSH_KEY ->
        EntrySecret(credential = SshCredential(privateKey = value), notes = notes)

    EntryType.ID_CARD, EntryType.PASSPORT, EntryType.DRIVER_LICENSE ->
        EntrySecret(credential = IdentityCredential(idNumber = value), notes = notes)

    EntryType.SEED_PHRASE ->
        EntrySecret(credential = IdentityCredential(seedPhrase = value), notes = notes)

    EntryType.RECOVERY_CODE ->
        EntrySecret(
            credential = IdentityCredential(
                recoveryCodes = value.lines().map(String::trim).filter(String::isNotEmpty)
            ),
            notes = notes
        )

    EntryType.PASSKEY ->
        EntrySecret(credential = PasskeyCredential(privateKeyReference = value), notes = notes)

    EntryType.OTP ->
        EntrySecret(credential = OtpCredential(OtpConfig(secret = value)), notes = notes)

    EntryType.ACCOUNT, EntryType.NOTE ->
        EntrySecret(credential = EntryCredential.None, notes = notes)

    else -> EntrySecret(credential = LoginCredential(password = value), notes = notes)
}

private fun String.toSummaryTags(): Set<String> =
    split(',', '，', ';', '；', '\n')
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .distinctBy { it.lowercase() }.toSet()
