package com.aozijx.passly.feature.vault.presentation.detail

import com.aozijx.passly.R
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
import com.aozijx.passly.domain.entry.model.query.EntryRelationsSnapshot
import com.aozijx.passly.domain.entry.model.relation.EntryLinkId
import com.aozijx.passly.domain.entry.model.relation.RelatedPasswordEntry
import com.aozijx.passly.domain.entry.model.sensitive.SensitiveFieldKey
import com.aozijx.passly.domain.sensitive.OwnedChars
import com.aozijx.passly.feature.vault.presentation.common.AppPackagePickerUiState
import com.aozijx.passly.feature.vault.presentation.detail.DetailUiState
import com.aozijx.passly.feature.vault.presentation.detail.model.DetailTitleUiState
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
            RelatedPasswordUnlinkUiState.Open(
                target = RelatedPasswordUnlinkTargetUiModel("target", "link", "Target"),
                phase = RelatedPasswordUnlinkPhase.AwaitingConfirmation,
            )

        val updated =
            DetailReducer.reduce(
                DetailUiState(snapshotState = DetailSnapshotState.Content(entry("current"))),
                DetailMutation.PasswordUnlinkStateChanged(confirming),
            )

        assertEquals(confirming, updated.passwordUnlink)
    }

    @Test
    fun relationRefreshUpdatesAllProjectionsOnlyForTheCurrentEntry() {
        val current = entry("current")
        val accountEntry = entry("account")
        val otherEntry = entry("other")
        val relatedPassword = RelatedPasswordEntry(EntryLinkId("link"), EntryId("target"), "Target", "user")
        val relations = EntryRelationsSnapshot(listOf(accountEntry), listOf(otherEntry), listOf(relatedPassword))
        val initial = DetailUiState(snapshotState = DetailSnapshotState.Content(current))

        val updated =
            DetailReducer.reduce(
                initial,
                DetailMutation.RelationsChanged(current.id, relations),
            )
        val stale =
            DetailReducer.reduce(
                updated,
                DetailMutation.RelationsChanged(EntryId("replacement"), EntryRelationsSnapshot(emptyList(), emptyList(), emptyList())),
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
                snapshotState = DetailSnapshotState.Content(entry("Before")),
                fieldEdits = DetailFieldEditState().start(DetailDraftKey.Title, " Canonical "),
                saveState = DetailSaveState.Saving(DetailEditCompletion.Title),
            )
        val presented = DetailReducer.reduce(saving, DetailMutation.EntryPresented(presentation(latest, emptyList())))
        val completed =
            DetailReducer.reduce(
                presented,
                DetailMutation.SaveSucceeded.DraftSaved(DetailEditCompletion.Title, "Canonical"),
            )
        assertFalse(completed.fieldEdits.edit(DetailDraftKey.Title) is DetailFieldEdit.Editing)
        assertEquals(DetailTitleUiState.Display("Canonical"), detailHeaderUiModel(latest, completed).title)
    }

    @Test
    fun `late entry presentation does not reopen a cancelled title edit`() {
        val latest = entry("Stored title")
        val editing =
            DetailUiState(
                snapshotState = DetailSnapshotState.Content(latest),
                fieldEdits = DetailFieldEditState().start(DetailDraftKey.Title, "Abandoned draft"),
            )
        val cancelled = DetailReducer.reduce(editing, DetailMutation.FieldEditingCancelled(DetailDraftKey.Title))
        val actual =
            DetailReducer.reduce(
                cancelled,
                DetailMutation.EntryPresented(
                    presentation = presentation(latest, emptyList()),
                ),
            )

        assertFalse(actual.fieldEdits.edit(DetailDraftKey.Title) is DetailFieldEdit.Editing)
        assertEquals(DetailTitleUiState.Display("Stored title"), detailHeaderUiModel(latest, actual).title)
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

        assertEquals(DetailTitleUiState.Display(" Actual SSID "), detailHeaderUiModel(wifi, actual).title)
        assertEquals(DetailSnapshotState.Content(wifi), actual.snapshotState)
    }

    @Test
    fun `presenting a saved entry keeps an active title draft`() {
        val latest = entry("Stored title")
        val state =
            DetailUiState(
                snapshotState = DetailSnapshotState.Content(latest),
                fieldEdits = DetailFieldEditState().start(DetailDraftKey.Title, "Local draft"),
            )
        val actual =
            DetailReducer.reduce(
                state,
                DetailMutation.EntryPresented(
                    presentation = presentation(latest, emptyList()),
                ),
            )

        assertEquals(
            DetailFieldEdit.Editing(initialValue = "Local draft", draft = "Local draft"),
            actual.fieldEdits.edit(DetailDraftKey.Title),
        )
    }

    @Test
    fun `title editing transitions are derived only from state and mutation`() {
        val entry = entry(title = "Original")
        val started =
            DetailReducer.reduce(
                DetailUiState(snapshotState = DetailSnapshotState.Content(entry)),
                DetailMutation.FieldEditingStarted(DetailDraftKey.Title, "Original"),
            )
        val changed =
            DetailReducer.reduce(
                started,
                DetailMutation.FieldDraftChanged(DetailDraftKey.Title, "Draft"),
            )
        val cancelled = DetailReducer.reduce(changed, DetailMutation.FieldEditingCancelled(DetailDraftKey.Title))

        assertEquals(
            DetailFieldEdit.Editing(initialValue = "Original", draft = "Original"),
            started.fieldEdits.edit(DetailDraftKey.Title),
        )
        assertEquals("Draft", (changed.fieldEdits.edit(DetailDraftKey.Title) as DetailFieldEdit.Editing).draft)
        assertFalse(cancelled.fieldEdits.edit(DetailDraftKey.Title) is DetailFieldEdit.Editing)
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
                DetailMutation.SaveSucceeded.DraftSaved(
                    DetailEditCompletion.Field(FieldKey.PASSWORD),
                    "new",
                ),
            )

        assertEquals(
            DetailFieldEdit.Editing(initialValue = "old", draft = "old"),
            started.fieldEdits.edit(DetailDraftKey.EntryField(FieldKey.PASSWORD)),
        )
        assertEquals(
            DetailFieldEdit.Editing(initialValue = "old", draft = "new"),
            changed.fieldEdits.edit(DetailDraftKey.EntryField(FieldKey.PASSWORD)),
        )
        assertEquals(DetailFieldEdit.NotEditing, saved.fieldEdits.edit(DetailDraftKey.EntryField(FieldKey.PASSWORD)))
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

        assertEquals(
            DetailFieldEdit.Editing(initialValue = "123", draft = "123"),
            failed.fieldEdits.edit(DetailDraftKey.EntryField(FieldKey.CARD_CVV)),
        )
    }

    @Test
    fun `revealed field mutations add and clear individual values`() {
        val cvv = OwnedChars.fromString("123")
        try {
            val revealed =
                DetailReducer.reduce(
                    DetailUiState(),
                    DetailMutation.RevealedFieldAdded(FieldKey.CARD_CVV, cvv),
                )
            val hidden =
                DetailReducer.reduce(
                    revealed,
                    DetailMutation.RevealedFieldCleared(FieldKey.CARD_CVV),
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
            assertEquals(DetailSnapshotState.Content(entry("Updated")), presented.snapshotState)
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
                        snapshotState = DetailSnapshotState.Content(entry("Secret")),
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
        val item =
            DetailInstalledApp(
                label = "Browser",
                packageName = "com.example.browser",
            )
        val state = DetailUiState(snapshotState = DetailSnapshotState.Content(entry("Current")))

        val associated =
            DetailReducer.reduce(
                state,
                DetailMutation.EntryPresented(
                    presentation = presentation((state.snapshotState as DetailSnapshotState.Content).entry, listOf(item)),
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
                DetailUiState(snapshotState = DetailSnapshotState.Content(entry("Current"))),
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
                    DetailFaviconSource.BuiltIn(
                        "security.key",
                        DetailFaviconSource.ColorSelection.Token("primary"),
                    ),
                ),
            )

        val editor = actual.faviconEditor as DetailFaviconEditorState.Open
        assertEquals(
            DetailFaviconSource.BuiltIn("security.key", DetailFaviconSource.ColorSelection.Token("primary")),
            editor.source,
        )
        assertFalse(editor.dirty)
    }

    @Test
    fun dirtyFaviconDismissConfirmsAndSaveFailurePreservesDraftWhileSuccessCloses() {
        val editor =
            DetailFaviconEditorState.Open(
                initialSource = DetailFaviconSource.InferredDefault,
                source =
                    DetailFaviconSource.BuiltIn(
                        "finance.bank",
                        DetailFaviconSource.ColorSelection.Token("secondary"),
                    ),
            )
        val requested =
            DetailFaviconEditorReducer.reduce(
                editor,
                DetailMutation.FaviconEditorDismissRequested,
            ) as DetailFaviconEditorState.Open
        val saving =
            DetailUiState(
                saveState = DetailSaveState.Saving(DetailEditCompletion.Icon),
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
                DetailMutation.SaveSucceeded.OtherSaved(DetailEditCompletion.Icon),
            )

        val failedEditor = failed.faviconEditor as DetailFaviconEditorState.Open
        assertTrue(requested.showDiscardConfirmation)
        assertEquals(editor.source, failedEditor.source)
        assertEquals(
            DetailFaviconWorkState.Failed(R.string.vault_detail_favicon_error_save_failed),
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
                        work = DetailFaviconWorkState.Failed(R.string.vault_detail_favicon_error_save_failed),
                    ),
            )

        val actual = DetailReducer.reduce(state, DetailMutation.SaveStarted(DetailEditCompletion.Icon))

        assertEquals(DetailSaveState.Saving(DetailEditCompletion.Icon), actual.saveState)
        assertEquals(DetailFaviconWorkState.Idle, (actual.faviconEditor as DetailFaviconEditorState.Open).work)
    }

    @Test
    fun promotedPrivateImageBecomesCurrentSourceAndSaveClosesEditor() {
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
        assertEquals(DetailFaviconCropInput.None, promoted.cropInput)

        val saved =
            DetailReducer.reduce(
                DetailUiState(
                    saveState = DetailSaveState.Saving(DetailEditCompletion.Icon),
                    faviconEditor = promoted,
                ),
                DetailMutation.SaveSucceeded.OtherSaved(DetailEditCompletion.Icon),
            )

        assertEquals(DetailFaviconEditorState.Closed, saved.faviconEditor)
    }

    @Test
    fun savingTagsRejectsEditsAndDismissalsUntilTheWriteCompletes() {
        val editor = DetailTagEditorState.Open(initialTags = setOf("Work"), input = "Personal")
        val saving = DetailUiState(tagEditor = editor, saveState = DetailSaveState.Saving(DetailEditCompletion.Tags))
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
        val succeeded = DetailReducer.reduce(saving, DetailMutation.SaveSucceeded.OtherSaved(DetailEditCompletion.Tags))
        assertEquals(DetailTagEditorState.Closed, succeeded.tagEditor)
    }

    @Test
    fun savingAnUnrelatedFieldDoesNotBlockTheTagDraft() {
        val state =
            DetailUiState(
                tagEditor = DetailTagEditorState.Open(initialTags = emptySet()),
                saveState = DetailSaveState.Saving(DetailEditCompletion.Favorite),
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
                saveState = DetailSaveState.Saving(DetailEditCompletion.Tags),
                tagEditor =
                    DetailTagEditorState.Open(
                        initialTags = setOf("Work"),
                        draftTags = setOf("Personal"),
                    ),
            )

        val failed = DetailReducer.reduce(saving, DetailMutation.SaveFailed(DetailEditCompletion.Tags))
        val succeeded = DetailReducer.reduce(saving, DetailMutation.SaveSucceeded.OtherSaved(DetailEditCompletion.Tags))

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
