package com.aozijx.passly.presentation.feature.vault.detail.component

import androidx.compose.runtime.Composable
import com.aozijx.passly.domain.entry.model.Entry
import com.aozijx.passly.presentation.feature.vault.detail.DetailEditCompletion
import com.aozijx.passly.feature.vault.detail.DetailEntryPatch
import com.aozijx.passly.presentation.feature.vault.detail.DetailUiAction
import com.aozijx.passly.presentation.feature.vault.detail.EntryEditState
import com.aozijx.passly.presentation.ui.vault.detail.component.AssociatedInfoSection
import com.aozijx.passly.presentation.ui.vault.detail.model.DetailAssociatedInfoUiModel

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
