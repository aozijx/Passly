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
import com.aozijx.passly.domain.entry.model.otp.OtpGenerationError
import com.aozijx.passly.domain.sensitive.OwnedChars
import com.aozijx.passly.feature.vault.model.OtpCodeState
import com.aozijx.passly.presentation.feature.vault.detail.ui.model.DetailActivityTypeUiModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DetailUiMapperTest {
    @Test
    fun headerMapperKeepsOnlyHeaderState() {
        val entry = noteEntry()
        val state = DetailUiState(
            entry = entry,
            editedTitle = "Edited",
            isEditingTitle = true,
        )

        val ui = detailHeaderUiModel(entry, state)

        assertEquals("Example", ui.title)
        assertTrue(ui.favorite)
        assertEquals("Edited", ui.editedTitle)
        assertTrue(ui.isEditingTitle)
    }

    @Test
    fun contentMapperProjectsOnlySharedContentModels() {
        val entry = noteEntry()
        val state = DetailUiState(
            entry = entry,
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

        val ui = detailContentUiModel(entry, state)

        assertEquals("related", ui.relatedEntries.single().id)
        assertEquals(1L, ui.metadata.createdAt)
        assertEquals(1L, ui.metadata.updatedAt)
        assertEquals(DetailActivityTypeUiModel.AUTOFILL, ui.activities.single().type)
        assertEquals("browser", ui.activities.single().source)
        assertEquals(3L, ui.activities.single().createdAt)
    }

    @Test
    fun otpMapperPreservesVolatileOtpState() {
        val ui = detailOtpUiModel(
            OtpCodeState(
                code = "123456",
                progress = 0.25f,
                isLoading = true,
                error = OtpGenerationError.InvalidSecret,
            )
        )

        requireNotNull(ui)
        assertEquals("123456", ui.code)
        assertEquals(0.25f, ui.progress)
        assertTrue(ui.isLoading)
        assertTrue(ui.hasError)
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

    @Test
    fun presentationMapperProjectsEditorOwnershipAndSaveProgress() {
        val entry = noteEntry()
        val state = DetailUiState(
            entry = entry,
            fieldEdits = DetailFieldEditState().start(DetailEditKey.NOTES, "draft"),
            savingEdit = DetailEditCompletion.Tags,
        )

        val presentation = requireNotNull(
            toDetailPresentationModel(state, null, "Username", "Password"),
        )

        assertTrue(presentation.content.notes.isEditing)
        assertEquals("draft", presentation.content.notes.editedNotes)
        assertTrue(presentation.overlays.savingTags)
        assertFalse(presentation.overlays.savingIcon)
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
