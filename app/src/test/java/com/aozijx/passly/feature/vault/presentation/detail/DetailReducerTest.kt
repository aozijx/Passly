package com.aozijx.passly.feature.vault.presentation.detail

import com.aozijx.passly.domain.entry.model.Entry
import com.aozijx.passly.domain.entry.model.EntryIdentity
import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.entry.model.FieldKey
import com.aozijx.passly.domain.entry.model.EntrySecret
import com.aozijx.passly.domain.entry.model.EntryProfile
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.model.EntryVersion
import com.aozijx.passly.domain.entry.model.EntryTimestamps
import com.aozijx.passly.domain.entry.model.credential.LoginCredential
import com.aozijx.passly.domain.entry.model.sensitive.SensitiveFieldKey
import com.aozijx.passly.domain.sensitive.OwnedChars
import com.aozijx.passly.feature.vault.presentation.detail.DetailUiState
import com.aozijx.passly.feature.vault.presentation.detail.section.DetailSectionKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DetailReducerTest {
    @Test
    fun `title editing transitions are derived only from state and mutation`() {
        val entry = entry(title = "Original")
        val started = DetailReducer.reduce(
            DetailUiState(entry = entry),
            DetailMutation.TitleEditingStarted,
        )
        val changed = DetailReducer.reduce(
            started,
            DetailMutation.EditedTitleChanged("Draft"),
        )
        val cancelled = DetailReducer.reduce(changed, DetailMutation.TitleEditingCancelled)

        assertTrue(started.isEditingTitle)
        assertEquals("Original", started.editedTitle)
        assertEquals("Draft", changed.editedTitle)
        assertFalse(cancelled.isEditingTitle)
        assertEquals("Original", cancelled.editedTitle)
    }

    @Test
    fun `field edit draft is owned by detail state until save succeeds`() {
        val started = DetailReducer.reduce(
            DetailUiState(),
            DetailMutation.FieldEditingStarted(DetailDraftKey.EntryField(FieldKey.PASSWORD), "old"),
        )
        val changed = DetailReducer.reduce(
            started,
            DetailMutation.FieldDraftChanged(DetailDraftKey.EntryField(FieldKey.PASSWORD), "new"),
        )
        val saving = DetailReducer.reduce(
            changed,
            DetailMutation.SaveStarted(
                DetailEditCompletion.Field(FieldKey.PASSWORD),
            ),
        )
        val saved = DetailReducer.reduce(
            saving,
            DetailMutation.SaveSucceeded(
                DetailEditCompletion.Field(FieldKey.PASSWORD),
            ),
        )

        assertTrue(started.fieldEdits.isEditing(FieldKey.PASSWORD))
        assertEquals("old", started.fieldEdits.draft(FieldKey.PASSWORD))
        assertEquals("new", changed.fieldEdits.draft(FieldKey.PASSWORD))
        assertFalse(saved.fieldEdits.isEditing(FieldKey.PASSWORD))
        assertEquals("", saved.fieldEdits.draft(FieldKey.PASSWORD))
    }

    @Test
    fun `failed field save keeps draft open for correction`() {
        val completion = DetailEditCompletion.Field(FieldKey.CARD_CVV)
        val editing = DetailReducer.reduce(
            DetailUiState(),
            DetailMutation.FieldEditingStarted(DetailDraftKey.EntryField(FieldKey.CARD_CVV), "123"),
        )
        val saving = DetailReducer.reduce(editing, DetailMutation.SaveStarted(completion))
        val failed = DetailReducer.reduce(
            saving,
            DetailMutation.SaveFailed(completion, "write_failed"),
        )

        assertTrue(failed.fieldEdits.isEditing(FieldKey.CARD_CVV))
        assertEquals("123", failed.fieldEdits.draft(FieldKey.CARD_CVV))
    }

    @Test
    fun `revealed field mutation adds and wipes individual values`() {        val cvv = OwnedChars.fromString("123")
        try {
            val revealed = DetailReducer.reduce(
                DetailUiState(),
                DetailMutation.RevealedFieldChanged(FieldKey.CARD_CVV, cvv),
            )
            val hidden = DetailReducer.reduce(
                revealed,
                DetailMutation.RevealedFieldChanged(FieldKey.CARD_CVV, null),
            )

            assertEquals("123", String(revealed.revealed(FieldKey.CARD_CVV)!!.toCharArray()))
            assertNull(hidden.revealed(FieldKey.CARD_CVV))
        } finally {
            cvv.wipe()
        }
    }

    @Test
    fun `entry presentation preserves orthogonal screen state`() {
        val password = OwnedChars.fromString("secret")
        try {
            val state = DetailUiState(
                isAccessHistoryEnabled = true,
                revealedFields = mapOf(FieldKey.PASSWORD to password),
            )
            val presented = DetailReducer.reduce(
                state,
                DetailMutation.EntryPresented(
                    presentation = presentation(
                        entry = entry("Updated"),
                        associatedApps = emptyList(),
                    ),
                    isEditingTitle = false,
                    editedTitle = "Updated",
                ),
            )

            assertTrue(presented.isAccessHistoryEnabled)
            assertEquals("secret", String(presented.revealed(FieldKey.PASSWORD)!!.toCharArray()))
            assertEquals("Updated", presented.entry?.title)
            assertEquals(listOf(DetailSectionKey.CREDENTIAL, DetailSectionKey.NOTES), presented.sections)
        } finally {
            password.wipe()
        }
    }

    @Test
    fun `state cleared removes entry and sensitive presentation state`() {
        val password = OwnedChars.fromString("secret")
        try {
            val cleared = DetailReducer.reduce(
                DetailUiState(
                    entry = entry("Secret"),
                    revealedFields = mapOf(FieldKey.PASSWORD to password),
                    saveState = DetailSaveState.Saving(DetailEditCompletion.Notes),
                ),
                DetailMutation.StateCleared,
            )

            assertEquals(DetailUiState(), cleared)
        } finally {
            password.wipe()
        }
    }

    @Test
    fun `application metadata updates only the currently presented entry`() {
        val item = DetailInstalledApp(
            label = "Browser",
            packageName = "com.example.browser",
        )
        val state = DetailUiState(entry = entry("Current"))

        val associated = DetailReducer.reduce(
            state,
            DetailMutation.EntryPresented(
                presentation = presentation(state.entry!!, listOf(item)),
                isEditingTitle = false,
                editedTitle = state.entry.title,
            ),
        )
        val stalePicker = DetailReducer.reduce(
            associated,
            DetailMutation.PackagePickerAppsChanged(EntryId("stale-entry"), listOf(item)),
        )

        assertEquals(listOf(item), associated.associatedApps)
        assertEquals(emptyList<Any>(), stalePicker.packagePickerApps)
        assertFalse(stalePicker.packagePickerAppsLoaded)
    }

    private fun presentation(
        entry: Entry,
        associatedApps: List<DetailInstalledApp>,
    ) = DetailEntryPresentation(
        entry = entry,
        sections = listOf(DetailSectionKey.CREDENTIAL, DetailSectionKey.NOTES),
        associatedApps = associatedApps,
        sensitiveFieldKeys = emptySet(),
    )

    @Test
    fun `package picker metadata marks the current entry load complete`() {
        val item = DetailInstalledApp(
            label = "Browser",
            packageName = "com.example.browser",
        )

        val actual = DetailReducer.reduce(
            DetailUiState(entry = entry("Current")),
            DetailMutation.PackagePickerAppsChanged(EntryId("entry-1"), listOf(item)),
        )

        assertEquals(listOf(item), actual.packagePickerApps)
        assertTrue(actual.packagePickerAppsLoaded)
    }
    private fun entry(title: String) = Entry(
        identity = EntryIdentity(
            id = EntryId("entry-1"),
            type = EntryType.LOGIN,
            version = EntryVersion.INITIAL,
            timestamps = EntryTimestamps(1L),
        ),
        profile = EntryProfile(title = title),
        secret = EntrySecret(credential = LoginCredential()),
    )
}
