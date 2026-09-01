package com.aozijx.passly.presentation.feature.vault.detail.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import com.aozijx.passly.R
import com.aozijx.passly.domain.entry.model.Entry
import com.aozijx.passly.presentation.feature.vault.detail.DetailEditCompletion
import com.aozijx.passly.presentation.feature.vault.detail.DetailEntryPatch
import com.aozijx.passly.presentation.feature.vault.detail.DetailUiAction
import com.aozijx.passly.presentation.feature.vault.detail.EntryEditState
import com.aozijx.passly.presentation.ui.vault.detail.component.AssociatedInfoSection
import com.aozijx.passly.presentation.ui.vault.detail.component.EntryTagsItem
import com.aozijx.passly.presentation.ui.vault.detail.component.InfoGroupCard
import com.aozijx.passly.presentation.ui.vault.detail.component.NotesSection
import com.aozijx.passly.presentation.ui.vault.detail.component.RelatedEntriesSection
import com.aozijx.passly.presentation.ui.vault.detail.model.DetailAssociatedInfoUiModel
import com.aozijx.passly.presentation.ui.vault.detail.model.DetailNotesUiModel
import com.aozijx.passly.presentation.ui.vault.detail.model.RelatedEntryUiModel

/** Maps feature state and intents to the passive supporting detail sections. */
@Composable
internal fun DetailRelatedEntriesHost(
    entries: List<Entry>,
    models: List<RelatedEntryUiModel>,
    onOpenEntry: (Entry) -> Unit,
) {
    RelatedEntriesSection(
        entries = models,
        onOpenEntry = { id -> entries.firstOrNull { it.id.value == id }?.let(onOpenEntry) },
    )
}

@Composable
internal fun DetailTagsHost(entry: Entry, onAction: (DetailUiAction) -> Unit) {
    InfoGroupCard(title = stringResource(R.string.vault_detail_tags_title)) {
        EntryTagsItem(
            tags = entry.tags,
            onClick = { onAction(DetailUiAction.OpenTagEditor) },
        )
    }
}

@Composable
internal fun DetailAssociationsHost(
    entry: Entry,
    editState: EntryEditState,
    onAction: (DetailUiAction) -> Unit,
) {
    AssociatedInfoSection(
        model = DetailAssociatedInfoUiModel(
            domain = entry.associatedDomain,
            applicationIds = entry.associations.applicationIds.sorted(),
            isEditingDomain = editState.isEditingDomain,
        ),
        onDomainEditStarted = { editState.isEditingDomain = true },
        onDomainChanged = { editState.editedDomain = it },
        onDomainSaved = {
            editState.editedDomain = it
            onAction(
                DetailUiAction.CommitPatch(
                    DetailEntryPatch.Associations(
                        primaryUrl = it.trim().ifBlank { null },
                        applicationIds = entry.associations.applicationIds,
                    ),
                    DetailEditCompletion.Associations,
                ),
            )
        },
        onPackageSelected = {
            editState.editedPackage = it
            onAction(
                DetailUiAction.CommitPatch(
                    DetailEntryPatch.Associations(
                        primaryUrl = entry.associations.primaryUrl,
                        applicationIds = setOf(it),
                    ),
                    DetailEditCompletion.Associations,
                ),
            )
        },
    )
}

@Composable
internal fun DetailNotesHost(
    entry: Entry,
    editState: EntryEditState,
    onAction: (DetailUiAction) -> Unit,
) {
    NotesSection(
        model = DetailNotesUiModel(
            notes = entry.secret.notes,
            editedNotes = editState.editedNotes.text,
            isEditing = editState.isEditingNotes,
        ),
        onEditStarted = { editState.startNotesEditing(entry.secret.notes) },
        onNotesChanged = { editState.editedNotes = TextFieldValue(it) },
        onNotesSaved = {
            editState.editedNotes = TextFieldValue(it)
            onAction(
                DetailUiAction.CommitPatch(
                    DetailEntryPatch.Notes(it.ifBlank { null }),
                    DetailEditCompletion.Notes,
                ),
            )
        },
    )
}
