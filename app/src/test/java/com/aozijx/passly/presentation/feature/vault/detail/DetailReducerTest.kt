package com.aozijx.passly.presentation.feature.vault.detail

import com.aozijx.passly.domain.entry.model.Entry
import com.aozijx.passly.domain.entry.model.EntryIdentity
import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.entry.model.EntrySecret
import com.aozijx.passly.domain.entry.model.EntryProfile
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.model.EntryVersion
import com.aozijx.passly.domain.entry.model.EntryTimestamps
import com.aozijx.passly.domain.entry.model.credential.LoginCredential
import com.aozijx.passly.domain.entry.model.sensitive.SensitiveFieldKey
import com.aozijx.passly.domain.sensitive.OwnedChars
import com.aozijx.passly.presentation.feature.vault.detail.DetailUiState
import com.aozijx.passly.presentation.feature.vault.detail.section.DetailSectionKey
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
            DetailMutation.FieldEditingStarted(RevealedFieldKey.PASSWORD, "old"),
        )
        val changed = DetailReducer.reduce(
            started,
            DetailMutation.FieldDraftChanged(RevealedFieldKey.PASSWORD, "new"),
        )
        val saving = DetailReducer.reduce(
            changed,
            DetailMutation.SaveStarted(
                DetailEditCompletion.SensitiveField(RevealedFieldKey.PASSWORD),
            ),
        )
        val saved = DetailReducer.reduce(
            saving,
            DetailMutation.SaveSucceeded(
                DetailEditCompletion.SensitiveField(RevealedFieldKey.PASSWORD),
            ),
        )

        assertTrue(started.fieldEdits.isEditing(RevealedFieldKey.PASSWORD))
        assertEquals("old", started.fieldEdits.draft(RevealedFieldKey.PASSWORD))
        assertEquals("new", changed.fieldEdits.draft(RevealedFieldKey.PASSWORD))
        assertFalse(saved.fieldEdits.isEditing(RevealedFieldKey.PASSWORD))
        assertEquals("", saved.fieldEdits.draft(RevealedFieldKey.PASSWORD))
    }

    @Test
    fun `failed field save keeps draft open for correction`() {
        val completion = DetailEditCompletion.SensitiveField(RevealedFieldKey.CVV)
        val editing = DetailReducer.reduce(
            DetailUiState(),
            DetailMutation.FieldEditingStarted(RevealedFieldKey.CVV, "123"),
        )
        val saving = DetailReducer.reduce(editing, DetailMutation.SaveStarted(completion))
        val failed = DetailReducer.reduce(
            saving,
            DetailMutation.SaveFailed(completion, "write_failed"),
        )

        assertTrue(failed.fieldEdits.isEditing(RevealedFieldKey.CVV))
        assertEquals("123", failed.fieldEdits.draft(RevealedFieldKey.CVV))
    }

    @Test
    fun `revealed field mutation adds and wipes individual values`() {        val cvv = OwnedChars.fromString("123")
        try {
            val revealed = DetailReducer.reduce(
                DetailUiState(),
                DetailMutation.RevealedFieldChanged("cvv", cvv),
            )
            val hidden = DetailReducer.reduce(
                revealed,
                DetailMutation.RevealedFieldChanged("cvv", null),
            )

            assertEquals("123", String(revealed.revealed("cvv")!!.toCharArray()))
            assertNull(hidden.revealed("cvv"))
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
                revealedFields = mapOf("password" to password),
            )
            val presented = DetailReducer.reduce(
                state,
                DetailMutation.EntryPresented(
                    entry = entry("Updated"),
                    entryType = EntryType.LOGIN,
                    strategySummary = "ready",
                    validationError = null,
                    strategyReady = true,
                    sections = listOf(DetailSectionKey.CREDENTIAL, DetailSectionKey.NOTES),
                    isEditingTitle = false,
                    editedTitle = "Updated",
                ),
            )

            assertTrue(presented.isAccessHistoryEnabled)
            assertEquals("secret", String(presented.revealed("password")!!.toCharArray()))
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
                    revealedFields = mapOf("password" to password),
                    savingEdit = DetailEditCompletion.Notes,
                ),
                DetailMutation.StateCleared,
            )

            assertEquals(DetailUiState(), cleared)
        } finally {
            password.wipe()
        }
    }

    @Test
    fun `late detail load result cannot update a different entry`() {
        val state = DetailUiState(
            entry = entry("Current"),
            sensitiveFieldKeys = setOf(SensitiveFieldKey.PASSWORD),
        )

        val actual = DetailReducer.reduce(
            state,
            DetailMutation.SensitiveFieldPresenceChanged(
                entryId = EntryId("stale-entry"),
                keys = emptySet(),
            ),
        )

        assertEquals(state, actual)
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
            DetailMutation.AssociatedAppsChanged(EntryId("entry-1"), listOf(item)),
        )
        val stalePicker = DetailReducer.reduce(
            associated,
            DetailMutation.PackagePickerAppsChanged(EntryId("stale-entry"), listOf(item)),
        )

        assertEquals(listOf(item), associated.associatedApps)
        assertEquals(emptyList<Any>(), stalePicker.packagePickerApps)
        assertFalse(stalePicker.packagePickerAppsLoaded)
    }

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
