package com.aozijx.passly.feature.vault.presentation.trash

import com.aozijx.passly.R
import com.aozijx.passly.domain.entry.model.EntryAssociations
import com.aozijx.passly.domain.entry.model.EntryIcon
import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.entry.model.EntryIdentity
import com.aozijx.passly.domain.entry.model.EntryProfile
import com.aozijx.passly.domain.entry.model.EntryTimestamps
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.model.EntryVersion
import com.aozijx.passly.domain.entry.model.query.EntryListItem
import com.aozijx.passly.feature.vault.presentation.trash.model.TrashDeletedAtUiState
import com.aozijx.passly.feature.vault.presentation.trash.model.TrashEntryListUiState
import com.aozijx.passly.feature.vault.presentation.trash.model.TrashEntryRowOperation
import com.aozijx.passly.feature.vault.presentation.trash.model.TrashSupportingTextUiState
import com.aozijx.passly.presentation.shared.entryvisual.EntryVisualCategory
import com.aozijx.passly.presentation.shared.entryvisual.VaultItemAssociatedAppUiState
import com.aozijx.passly.presentation.shared.entryvisual.VaultItemIconImageUiState
import com.aozijx.passly.presentation.shared.media.LocalIconImageModel
import org.junit.Assert.assertEquals
import org.junit.Test

class TrashUiMapperTest {
    @Test
    fun `maps deleted domain entries to ui-only trash items`() {
        val entry =
            EntryListItem(
                identity =
                    EntryIdentity(
                        id = EntryId("entry-1"),
                        type = EntryType.LOGIN,
                        version = EntryVersion(3),
                        timestamps = EntryTimestamps(10L, deletedAtMs = 20L),
                    ),
                profile =
                    EntryProfile(
                        title = "Mail",
                        username = "person@example.com",
                        associations =
                            EntryAssociations(
                                primaryUrl = "example.com",
                                applicationIds = setOf("com.example"),
                            ),
                        icon = EntryIcon(customReference = "icons/mail.png"),
                    ),
            )

        val screenState = TrashUiMapper.map(TrashUiState(content = TrashContentState.Items(listOf(entry))))
        val entries = screenState.entries as TrashEntryListUiState.Items
        val result = entries.entries.single()

        assertEquals("entry-1", result.id)
        assertEquals(3, result.version)
        assertEquals(TrashEntryRowOperation.Idle, result.operation)
        assertEquals("Mail", result.title)
        assertEquals(R.string.entry_type_login, result.entryTypeLabelRes)
        assertEquals(TrashSupportingTextUiState.Value("person@example.com"), result.supportingText)
        assertEquals(TrashDeletedAtUiState.Recorded(20L), result.deletedAt)
        assertEquals(VaultItemAssociatedAppUiState.Package("com.example"), result.icon.associatedApp)
        assertEquals(
            VaultItemIconImageUiState.Custom(LocalIconImageModel("file://icons/mail.png")),
            result.icon.image,
        )
        assertEquals(EntryVisualCategory.EMAIL, result.icon.visualCategory)
    }

    @Test
    fun `maps missing supporting and deletion values explicitly`() {
        val entry =
            EntryListItem(
                identity =
                    EntryIdentity(
                        id = EntryId("entry-2"),
                        type = EntryType.LOGIN,
                        version = EntryVersion(1),
                        timestamps = EntryTimestamps(10L),
                    ),
                profile = EntryProfile(title = "Mail", username = ""),
            )

        val screenState = TrashUiMapper.map(TrashUiState(content = TrashContentState.Items(listOf(entry))))
        val result = (screenState.entries as TrashEntryListUiState.Items).entries.single()

        assertEquals(TrashSupportingTextUiState.Absent, result.supportingText)
        assertEquals(TrashDeletedAtUiState.Unavailable, result.deletedAt)
    }
}
