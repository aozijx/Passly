package com.aozijx.passly.feature.vault.editor.password

import com.aozijx.passly.domain.entry.model.EntryHeader
import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.entry.model.EntrySecret
import com.aozijx.passly.domain.entry.model.EntrySummary
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.model.EntryVersion
import com.aozijx.passly.domain.entry.model.VaultEntry
import com.aozijx.passly.domain.entry.model.WebsiteInfo
import com.aozijx.passly.domain.entry.model.secret.LoginSecret

internal object PasswordEntryFactory {

    fun create(
        state: AddPasswordUiState,
        now: Long = System.currentTimeMillis()
    ): VaultEntry = VaultEntry(
        header = EntryHeader(
            id = EntryId(""),
            entryType = EntryType.LOGIN,
            version = EntryVersion.INITIAL,
            createdAt = now,
            updatedAt = now
        ),
        summary = EntrySummary(
            title = state.title.trim(),
            username = state.username.trim(),
            website = state.website.trim()
                .takeIf(String::isNotEmpty)
                ?.let { WebsiteInfo(primaryUrl = it) }
        ),
        secret = EntrySecret(
            login = LoginSecret(password = state.password),
            notes = state.notes.trim().takeIf(String::isNotEmpty)
        )
    )
}
