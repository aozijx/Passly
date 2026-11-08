package com.aozijx.passly.feature.vault.presentation.trash

import com.aozijx.passly.domain.entry.model.EntryAssociations
import com.aozijx.passly.domain.entry.model.EntryIcon
import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.entry.model.EntryIdentity
import com.aozijx.passly.domain.entry.model.EntryProfile
import com.aozijx.passly.domain.entry.model.EntryTimestamps
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.model.EntryVersion
import com.aozijx.passly.domain.entry.model.query.EntryListItem
import com.aozijx.passly.presentation.shared.components.EntryVisualCategory
import com.aozijx.passly.presentation.shared.media.LocalIconImageModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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

        val result = TrashUiMapper.map(listOf(entry)).single()

        assertEquals("entry-1", result.id)
        assertEquals(3, result.version)
        assertEquals("Mail", result.title)
        assertEquals(EntryType.LOGIN, result.entryType)
        assertEquals("person@example.com", result.username)
        assertEquals(20L, result.deletedAtEpochMs)
        assertEquals("example.com", result.associatedDomain)
        assertEquals("com.example", result.associatedAppPackage)
        assertNull(result.iconName)
        assertEquals("icons/mail.png", result.iconCustomPath)
        assertEquals(LocalIconImageModel("file://icons/mail.png"), result.iconImageModel)
        assertEquals(EntryVisualCategory.EMAIL, result.visualCategory)
    }
}
