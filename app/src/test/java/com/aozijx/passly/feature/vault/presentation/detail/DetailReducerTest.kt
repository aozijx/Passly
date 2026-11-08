package com.aozijx.passly.feature.vault.presentation.detail

import com.aozijx.passly.domain.entry.model.Entry
import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.entry.model.EntryIdentity
import com.aozijx.passly.domain.entry.model.EntryProfile
import com.aozijx.passly.domain.entry.model.EntrySecret
import com.aozijx.passly.domain.entry.model.EntryTimestamps
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.model.EntryVersion
import com.aozijx.passly.domain.entry.model.FieldKey
import com.aozijx.passly.domain.entry.model.credential.LoginCredential
import com.aozijx.passly.domain.entry.model.credential.WifiCredential
import com.aozijx.passly.domain.entry.model.relation.EntryLinkId
import com.aozijx.passly.domain.entry.model.sensitive.SensitiveFieldKey
import com.aozijx.passly.domain.sensitive.OwnedChars
import com.aozijx.passly.feature.vault.detail.DetailRelatedPassword
import com.aozijx.passly.feature.vault.detail.DetailRelationsSnapshot
import com.aozijx.passly.feature.vault.presentation.common.AppPackagePickerUiState
import com.aozijx.passly.feature.vault.presentation.detail.DetailUiState
import com.aozijx.passly.feature.vault.presentation.detail.section.DetailSectionKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DetailReducerTest {
    @Test
    fun passwordUnlinkMutationUpdatesTheCurrentState() {
        val confirming =
            RelatedPasswordUnlinkUiState.AwaitingConfirmation(
                target = RelatedPasswordUnlinkTargetUiModel("target", "link", "Target"),
            )

        val updated =
            DetailReducer.reduce(
                DetailUiState(entry = entry("current")),
                DetailMutation.PasswordUnlinkStateChanged(confirming),
            )

        assertEquals(confirming, updated.passwordUnlink)
    }

    @Test
    fun relationRefreshUpdatesAllProjectionsOnlyForTheCurrentEntry() {
        val current = entry("current")
        val accountEntry = entry("account")
        val otherEntry = entry("other")
        val relatedPassword = DetailRelatedPassword(EntryLinkId("link"), EntryId("target"), "Target", "user")
        val relations = DetailRelationsSnapshot(listOf(accountEntry), listOf(otherEntry), listOf(relatedPassword))
        val initial = DetailUiState(entry = current)

        val updated =
            DetailReducer.reduce(
                initial,
                DetailMutation.RelationsChanged(current.id, relations),
            )
        val stale =
            DetailReducer.reduce(
                updated,
                DetailMutation.RelationsChanged(EntryId("replacement"), DetailRelationsSnapshot(emptyList(), emptyList(), emptyList())),
            )

        assertEquals(listOf(accountEntry), updated.accountGroupEntries)
        assertEquals(listOf(otherEntry), updated.otherRelatedEntries)
        assertEquals(listOf(relatedPassword), updated.relatedPasswords)
        assertEquals(updated, stale)
    }

    @Test
    fun unchangedTitleCompletionUsesTheStoredDisplayTitle() {
        val latest = entry("Canonical")
        val saving =
            DetailUiState(
                entry = entry("Before"),
                isEditingTitle = true,
                editedTitle = " Canonical ",
                saveProgress = DetailSaveProgress(DetailEditCompletion.Title),
            )
        val presented = DetailReducer.reduce(saving, DetailMutation.EntryPresented(presentation(latest, emptyList())))
        val completed = DetailReducer.reduce(presented, DetailMutation.SaveSucceeded(DetailEditCompletion.Title))
        assertFalse(completed.isEditingTitle)
        assertEquals("Canonical", completed.editedTitle)
    }

    @Test
    fun `late entry presentation does not reopen a cancelled title edit`() {
        val latest = entry("Stored title")
        val editing = DetailUiState(entry = latest, isEditingTitle = true, editedTitle = "Abandoned draft")
        val cancelled = DetailReducer.reduce(editing, DetailMutation.TitleEditingCancelled)
        val actual =
            DetailReducer.reduce(
                cancelled,
                DetailMutation.EntryPresented(
                    presentation = presentation(latest, emptyList()),
                ),
            )

        assertFalse(actual.isEditingTitle)
        assertEquals("Stored title", actual.editedTitle)
    }

    @Test
    fun `presenting wifi after save restores network name instead of profile title`() {
        val wifi =
            entry("Personal label").let {
                it.copy(identity = it.identity.copy(type = EntryType.WIFI), secret = EntrySecret(WifiCredential(" Actual SSID ")))
            }
        val actual =
            DetailReducer.reduce(
                DetailUiState(),
                DetailMutation.EntryPresented(
                    presentation = presentation(wifi, emptyList()),
                ),
            )

        assertEquals(" Actual SSID ", actual.editedTitle)
        assertEquals(wifi, actual.entry)
    }

    @Test
    fun `presenting a saved entry keeps an active title draft`() {
        val latest = entry("Stored title")
        val state = DetailUiState(entry = latest, isEditingTitle = true, editedTitle = "Local draft")
        val actual =
            DetailReducer.reduce(
                state,
                DetailMutation.EntryPresented(
                    presentation = presentation(latest, emptyList()),
                ),
            )

        assertTrue(actual.isEditingTitle)
        assertEquals("Local draft", actual.editedTitle)
    }

    @Test
    fun `title editing transitions are derived only from state and mutation`() {
        val entry = entry(title = "Original")
        val started =
            DetailReducer.reduce(
                DetailUiState(entry = entry),
                DetailMutation.TitleEditingStarted,
            )
        val changed =
            DetailReducer.reduce(
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
        val started =
            DetailReducer.reduce(
                DetailUiState(),
                DetailMutation.FieldEditingStarted(DetailDraftKey.EntryField(FieldKey.PASSWORD), "old"),
            )
        val changed =
            DetailReducer.reduce(
                started,
                DetailMutation.FieldDraftChanged(DetailDraftKey.EntryField(FieldKey.PASSWORD), "new"),
            )
        val saving =
            DetailReducer.reduce(
                changed,
                DetailMutation.SaveStarted(
                    DetailEditCompletion.Field(FieldKey.PASSWORD),
                ),
            )
        val saved =
            DetailReducer.reduce(
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
        val editing =
            DetailReducer.reduce(
                DetailUiState(),
                DetailMutation.FieldEditingStarted(DetailDraftKey.EntryField(FieldKey.CARD_CVV), "123"),
            )
        val saving = DetailReducer.reduce(editing, DetailMutation.SaveStarted(completion))
        val failed =
            DetailReducer.reduce(
                saving,
                DetailMutation.SaveFailed(completion),
            )

        assertTrue(failed.fieldEdits.isEditing(FieldKey.CARD_CVV))
        assertEquals("123", failed.fieldEdits.draft(FieldKey.CARD_CVV))
    }

    @Test
    fun `revealed field mutation adds and wipes individual values`() {
        val cvv = OwnedChars.fromString("123")
        try {
            val revealed =
                DetailReducer.reduce(
                    DetailUiState(),
                    DetailMutation.RevealedFieldChanged(FieldKey.CARD_CVV, cvv),
                )
            val hidden =
                DetailReducer.reduce(
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
            val state =
                DetailUiState(
                    isAccessHistoryEnabled = true,
                    revealedFields = mapOf(FieldKey.PASSWORD to password),
                )
            val presented =
                DetailReducer.reduce(
                    state,
                    DetailMutation.EntryPresented(
                        presentation =
                            presentation(
                                entry = entry("Updated"),
                                associatedApps = emptyList(),
                            ),
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
            val cleared =
                DetailReducer.reduce(
                    DetailUiState(
                        entry = entry("Secret"),
                        revealedFields = mapOf(FieldKey.PASSWORD to password),
                        saveProgress = DetailSaveProgress(DetailEditCompletion.Notes),
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
        val item =
            DetailInstalledApp(
                label = "Browser",
                packageName = "com.example.browser",
            )
        val state = DetailUiState(entry = entry("Current"))

        val associated =
            DetailReducer.reduce(
                state,
                DetailMutation.EntryPresented(
                    presentation = presentation(state.entry!!, listOf(item)),
                ),
            )
        val stalePicker =
            DetailReducer.reduce(
                associated,
                DetailMutation.PackagePickerAppsChanged(EntryId("stale-entry"), listOf(item)),
            )

        assertEquals(listOf(item), associated.associatedApps)
        assertEquals(AppPackagePickerUiState.NotRequested, stalePicker.packagePickerApps)
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
        val item =
            DetailInstalledApp(
                label = "Browser",
                packageName = "com.example.browser",
            )

        val actual =
            DetailReducer.reduce(
                DetailUiState(entry = entry("Current")),
                DetailMutation.PackagePickerAppsChanged(EntryId("entry-1"), listOf(item)),
            )

        val matches = actual.packagePickerApps as AppPackagePickerUiState.Matches
        assertEquals(listOf(item), matches.installedApps)
    }

    @Test
    fun openMapsBuiltInIconToSingleDraftSource() {
        val actual =
            DetailReducer.reduce(
                DetailUiState(),
                DetailMutation.FaviconEditorOpened(
                    DetailFaviconSource.BuiltIn("security.key", "primary"),
                ),
            )

        val editor = actual.faviconEditor as DetailFaviconEditorState.Open
        assertEquals(DetailFaviconSource.BuiltIn("security.key", "primary"), editor.source)
        assertFalse(editor.dirty)
    }

    @Test
    fun dirtyFaviconDismissConfirmsAndSaveFailurePreservesDraftWhileSuccessCloses() {
        val editor =
            DetailFaviconEditorState.Open(
                initialSource = DetailFaviconSource.InferredDefault,
                source = DetailFaviconSource.BuiltIn("finance.bank", "secondary"),
            )
        val requested =
            DetailFaviconEditorReducer.reduce(
                editor,
                DetailMutation.FaviconEditorDismissRequested,
            ) as DetailFaviconEditorState.Open
        val saving =
            DetailUiState(
                saveProgress = DetailSaveProgress(DetailEditCompletion.Icon),
                faviconEditor = requested,
            )

        val failed =
            DetailReducer.reduce(
                saving,
                DetailMutation.SaveFailed(DetailEditCompletion.Icon),
            )
        val succeeded =
            DetailReducer.reduce(
                saving,
                DetailMutation.SaveSucceeded(DetailEditCompletion.Icon),
            )

        val failedEditor = failed.faviconEditor as DetailFaviconEditorState.Open
        assertTrue(requested.showDiscardConfirmation)
        assertEquals(editor.source, failedEditor.source)
        assertEquals(
            DetailFaviconWorkState.Failed(DetailFaviconProcessingError.SAVE_FAILED),
            failedEditor.work,
        )
        assertEquals(DetailFaviconEditorState.Closed, succeeded.faviconEditor)
    }

    @Test
    fun iconSaveStartClearsPreviousEditorError() {
        val state =
            DetailUiState(
                faviconEditor =
                    DetailFaviconEditorState.Open(
                        initialSource = DetailFaviconSource.InferredDefault,
                        work = DetailFaviconWorkState.Failed(DetailFaviconProcessingError.SAVE_FAILED),
                    ),
            )

        val actual = DetailReducer.reduce(state, DetailMutation.SaveStarted(DetailEditCompletion.Icon))

        assertEquals(DetailSaveProgress(DetailEditCompletion.Icon), actual.saveProgress)
        assertEquals(DetailFaviconWorkState.Idle, (actual.faviconEditor as DetailFaviconEditorState.Open).work)
    }

    @Test
    fun promotedPrivateImageKeepsCandidatePathUntilSaveCompletes() {
        val state =
            DetailFaviconEditorState.Open(
                initialSource = DetailFaviconSource.InferredDefault,
                source = DetailFaviconSource.PrivateImage("/private/staging/icon.webp"),
            )

        val promoted =
            DetailFaviconEditorReducer.reduce(
                state,
                DetailMutation.FaviconSourcePromoted("/private/images/favicon.webp"),
            ) as DetailFaviconEditorState.Open
        assertEquals(DetailFaviconSource.PrivateImage("/private/images/favicon.webp"), promoted.source)
        assertEquals("/private/images/favicon.webp", promoted.promotedCandidatePath)

        val saved =
            DetailReducer.reduce(
                DetailUiState(
                    saveProgress = DetailSaveProgress(DetailEditCompletion.Icon),
                    faviconEditor = promoted,
                ),
                DetailMutation.SaveSucceeded(DetailEditCompletion.Icon),
            )

        assertEquals(DetailFaviconEditorState.Closed, saved.faviconEditor)
    }

    @Test
    fun savingTagsRejectsEditsAndDismissalsUntilTheWriteCompletes() {
        val editor = DetailTagEditorState.Open(initialTags = setOf("Work"), input = "Personal")
        val saving = DetailUiState(tagEditor = editor, saveProgress = DetailSaveProgress(DetailEditCompletion.Tags))
        val changes =
            listOf(
                DetailMutation.TagInputChanged("New draft"),
                DetailMutation.TagSubmitted("Finance"),
                DetailMutation.TagRemoved("Work"),
                DetailMutation.TagEditorDismissRequested,
                DetailMutation.TagEditorDiscardConfirmed,
                DetailMutation.TagEditorDiscardCancelled,
                DetailMutation.TagEditorOpened(setOf("Other"), emptySet()),
            )
        for (change in changes) assertEquals(saving, DetailReducer.reduce(saving, change))
        val failed = DetailReducer.reduce(saving, DetailMutation.SaveFailed(DetailEditCompletion.Tags))
        val edited = DetailReducer.reduce(failed, DetailMutation.TagInputChanged("Try again"))
        assertEquals("Try again", (edited.tagEditor as DetailTagEditorState.Open).input)
        val succeeded = DetailReducer.reduce(saving, DetailMutation.SaveSucceeded(DetailEditCompletion.Tags))
        assertEquals(DetailTagEditorState.Closed, succeeded.tagEditor)
    }

    @Test
    fun savingAnUnrelatedFieldDoesNotBlockTheTagDraft() {
        val state =
            DetailUiState(
                tagEditor = DetailTagEditorState.Open(initialTags = emptySet()),
                saveProgress = DetailSaveProgress(DetailEditCompletion.Favorite),
            )

        val edited = DetailReducer.reduce(state, DetailMutation.TagInputChanged("Personal"))

        assertEquals("Personal", (edited.tagEditor as DetailTagEditorState.Open).input)
    }

    @Test
    fun openTagEditorInitializesSortedDraftAndAvailableSuggestions() {
        val actual =
            DetailReducer.reduce(
                DetailUiState(),
                DetailMutation.TagEditorOpened(
                    currentTags = setOf("Work", "Personal"),
                    availableTags = setOf("Password", "Finance"),
                ),
            )

        val editor = actual.tagEditor as DetailTagEditorState.Open
        assertEquals(linkedSetOf("Personal", "Work"), editor.initialTags)
        assertEquals(linkedSetOf("Personal", "Work"), editor.draftTags)
        assertEquals(setOf("Password", "Finance"), editor.availableTags)
        assertFalse(editor.dirty)
    }

    @Test
    fun successfulTagSaveClosesSheetButFailurePreservesDraft() {
        val saving =
            DetailUiState(
                saveProgress = DetailSaveProgress(DetailEditCompletion.Tags),
                tagEditor =
                    DetailTagEditorState.Open(
                        initialTags = setOf("Work"),
                        draftTags = setOf("Personal"),
                    ),
            )

        val failed = DetailReducer.reduce(saving, DetailMutation.SaveFailed(DetailEditCompletion.Tags))
        val succeeded = DetailReducer.reduce(saving, DetailMutation.SaveSucceeded(DetailEditCompletion.Tags))

        assertEquals(setOf("Personal"), (failed.tagEditor as DetailTagEditorState.Open).draftTags)
        assertEquals(DetailTagEditorState.Closed, succeeded.tagEditor)
    }

    private fun entry(title: String) =
        Entry(
            identity =
                EntryIdentity(
                    id = EntryId("entry-1"),
                    type = EntryType.LOGIN,
                    version = EntryVersion.INITIAL,
                    timestamps = EntryTimestamps(1L),
                ),
            profile = EntryProfile(title = title),
            secret = EntrySecret(credential = LoginCredential()),
        )
}
