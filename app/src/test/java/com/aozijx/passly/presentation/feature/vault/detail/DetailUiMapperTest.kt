package com.aozijx.passly.presentation.feature.vault.detail

import com.aozijx.passly.domain.entry.model.Entry
import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.entry.model.EntryIdentity
import com.aozijx.passly.domain.entry.model.EntryProfile
import com.aozijx.passly.domain.entry.model.EntrySecret
import com.aozijx.passly.domain.entry.model.EntryTimestamps
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.model.activity.ActivityType
import com.aozijx.passly.domain.entry.model.activity.EntryActivity
import com.aozijx.passly.domain.sensitive.OwnedChars
import com.aozijx.passly.presentation.feature.vault.detail.section.DetailSectionResolver
import com.aozijx.passly.presentation.ui.vault.detail.model.DetailEntryTypeUiModel
import com.aozijx.passly.presentation.ui.vault.detail.model.DetailActivityTypeUiModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DetailUiMapperTest {
    @Test
    fun screenMapperPreservesHeaderStateAndResolvedSectionOrder() {
        val entry = noteEntry()
        val state = DetailUiState(
            entry = entry,
            editedTitle = "Edited",
            isEditingTitle = true,
            validationError = "invalid",
            isAccessHistoryEnabled = true,
            relatedEntries = listOf(entry.copy(identity = entry.identity.copy(id = EntryId("related")))),
            history = listOf(
                EntryActivity(
                    entryId = entry.id.value,
                    activityType = ActivityType.AUTOFILL,
                    source = "browser",
                    createdAt = 3L,
                )
            ),
        )

        val ui = detailScreenUiModel(entry, state, otp = null)

        assertEquals("entry", ui.entryId)
        assertEquals("Example", ui.title)
        assertEquals("user", ui.username)
        assertEquals(DetailEntryTypeUiModel.NOTE, ui.entryType)
        assertEquals("Edited", ui.editedTitle)
        assertTrue(ui.isEditingTitle)
        assertTrue(ui.isAccessHistoryEnabled)
        assertEquals("related", ui.relatedEntries.single().id)
        assertEquals(1L, ui.metadata.createdAt)
        assertEquals(1L, ui.metadata.updatedAt)
        assertEquals(DetailActivityTypeUiModel.AUTOFILL, ui.activities.single().type)
        assertEquals("browser", ui.activities.single().source)
        assertEquals(3L, ui.activities.single().createdAt)
        assertEquals(
            DetailSectionResolver.resolve(entry).map { it.name },
            ui.sections.map { it.kind.name },
        )
    }

    @Test
    fun scopedSensitiveTextRedactsToStringAndWipesTemporaryCopy() {
        val source = OwnedChars.fromString("secret-value")
        val scoped = source.asScopedSensitiveText()
        lateinit var borrowed: CharArray

        val length = scoped.useChars {
            borrowed = it
            assertEquals("secret-value", String(it))
            it.size
        }

        assertEquals(12, length)
        assertEquals("***", scoped.toString())
        assertTrue(borrowed.all { it == '\u0000' })
        assertFalse(source.isWiped)
        source.close()
    }

    private fun noteEntry() = Entry(
        identity = EntryIdentity(
            id = EntryId("entry"),
            type = EntryType.NOTE,
            timestamps = EntryTimestamps(1L),
        ),
        profile = EntryProfile(title = "Example", username = "user", favorite = true),
        secret = EntrySecret(notes = "note"),
    )
}
