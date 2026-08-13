package com.aozijx.passly.feature.vault.editor.password

import com.aozijx.passly.domain.entry.model.EntryHeader
import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.entry.model.EntrySecret
import com.aozijx.passly.domain.entry.model.EntrySummary
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.model.EntryVersion
import com.aozijx.passly.domain.entry.model.EntryAggregate
import com.aozijx.passly.domain.entry.model.WebsiteInfo
import com.aozijx.passly.domain.entry.model.secret.LoginSecret

internal object PasswordEntryFactory {

    fun create(
        state: AddPasswordFormState,
        now: Long = System.currentTimeMillis()
    ): EntryAggregate = EntryAggregate(
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
                ?.let { WebsiteInfo(primaryUrl = it) },
            tags = state.tags.split(",")
                .map { it.trim() }
                .filter { it.isNotEmpty() }
        ),
        secret = EntrySecret(
            login = LoginSecret(password = state.password),
            notes = state.notes.trim().takeIf(String::isNotEmpty)
        )
    )
}
