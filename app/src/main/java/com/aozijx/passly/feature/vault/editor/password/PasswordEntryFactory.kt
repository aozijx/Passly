package com.aozijx.passly.feature.vault.editor.password

import com.aozijx.passly.domain.entry.model.EntryIdentity
import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.entry.model.EntrySecret
import com.aozijx.passly.domain.entry.model.EntryProfile
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.model.EntryVersion
import com.aozijx.passly.domain.entry.model.EntryTimestamps
import com.aozijx.passly.domain.entry.model.Entry
import com.aozijx.passly.domain.entry.model.EntryAssociations
import com.aozijx.passly.domain.entry.model.credential.LoginCredential
import com.github.f4b6a3.uuid.UuidCreator

internal object PasswordEntryFactory {

    fun create(
        state: AddPasswordFormState,
        now: Long = System.currentTimeMillis()
    ): Entry = Entry(
        identity = EntryIdentity(
            id = EntryId(UuidCreator.getTimeOrderedEpoch().toString()),
            type = EntryType.LOGIN,
            version = EntryVersion.INITIAL,
            timestamps = EntryTimestamps(now),
        ),
        profile = EntryProfile(
            title = state.title.trim(),
            username = state.username.trim(),
            associations = state.website.trim()
                .takeIf(String::isNotEmpty)
                ?.let { EntryAssociations(primaryUrl = it) }
                ?: EntryAssociations(),
            tags = state.tags.split(",")
                .map { it.trim() }
                .filter { it.isNotEmpty() }.toSet()
        ),
        secret = EntrySecret(
            credential = LoginCredential(password = state.password),
            notes = state.notes.trim().takeIf(String::isNotEmpty)
        )
    )
}
