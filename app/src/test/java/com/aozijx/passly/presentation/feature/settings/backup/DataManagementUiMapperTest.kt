package com.aozijx.passly.presentation.feature.settings.backup

import com.aozijx.passly.domain.entry.model.EntryAssociations
import com.aozijx.passly.domain.entry.model.EntryIcon
import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.entry.model.EntryIdentity
import com.aozijx.passly.domain.entry.model.EntryProfile
import com.aozijx.passly.domain.entry.model.EntryTimestamps
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.model.EntryVersion
import com.aozijx.passly.domain.entry.model.query.EntryListItem
import com.aozijx.passly.presentation.ui.shared.entry.EntryTypeUiModel
import org.junit.Assert.assertEquals
import org.junit.Test

class DataManagementUiMapperTest {

    @Test
    fun `maps deleted domain entries to ui-only trash items`() {
        val entry = EntryListItem(
            identity = EntryIdentity(
                id = EntryId("entry-1"),
                type = EntryType.LOGIN,
                version = EntryVersion(3),
                timestamps = EntryTimestamps(10L, deletedAtMs = 20L),
            ),
            profile = EntryProfile(
                title = "Mail",
                username = "person@example.com",
                associations = EntryAssociations(
                    primaryUrl = "example.com",
                    applicationIds = setOf("com.example"),
                ),
                icon = EntryIcon(customReference = "icons/mail.png"),
            ),
        )

        val result = DataManagementSettingsUiState(deletedEntries = listOf(entry)).toDetailState()
            .deletedEntries.single()

        assertEquals("entry-1", result.id)
        assertEquals(3, result.version)
        assertEquals(EntryTypeUiModel.LOGIN, result.entryType)
        assertEquals(20L, result.deletedAtEpochMs)
        assertEquals("example.com", result.associatedDomain)
        assertEquals("com.example", result.associatedAppPackage)
        assertEquals("icons/mail.png", result.iconCustomPath)
    }
}
