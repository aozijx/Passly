package com.aozijx.passly.presentation.feature.vault.detail.binding

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.aozijx.passly.core.platform.packageinfo.rememberInstalledAppIconBitmap
import com.aozijx.passly.domain.entry.model.Entry
import com.aozijx.passly.presentation.feature.vault.detail.DetailEditKey
import com.aozijx.passly.presentation.feature.vault.detail.DetailUiAction
import com.aozijx.passly.presentation.feature.vault.detail.DetailUiState
import com.aozijx.passly.presentation.ui.shared.components.AppPackagePickerBottomSheet
import com.aozijx.passly.presentation.ui.vault.detail.component.AssociatedInfoSection
import com.aozijx.passly.presentation.ui.vault.detail.model.DetailAssociatedInfoUiModel

@Composable
internal fun DetailAssociationsBinding(
    entry: Entry,
    uiState: DetailUiState,
    onAction: (DetailUiAction) -> Unit,
) {
    var showPackagePicker by remember { mutableStateOf(false) }

    AssociatedInfoSection(
        model = DetailAssociatedInfoUiModel(
            domain = entry.associatedDomain,
            editedDomain = uiState.fieldEdits.draft(DetailEditKey.DOMAIN),
            isEditingDomain = uiState.fieldEdits.isEditing(DetailEditKey.DOMAIN),
        ),
        associatedApps = uiState.associatedApps,
        appIcon = { packageName -> rememberInstalledAppIconBitmap(packageName) },
        onDomainEditStarted = { onAction(DetailUiAction.StartDomainEdit) },
        onDomainChanged = { onAction(DetailUiAction.UpdateDomainDraft(it)) },
        onDomainSaved = { onAction(DetailUiAction.SaveDomain) },
        onPackagePickerRequested = {
            showPackagePicker = true
            onAction(DetailUiAction.LoadPackagePickerApps)
        },
    )

    if (showPackagePicker) {
        AppPackagePickerBottomSheet(
            apps = uiState.packagePickerApps,
            appIcon = { packageName -> rememberInstalledAppIconBitmap(packageName) },
            onSelect = { app ->
                showPackagePicker = false
                onAction(DetailUiAction.SelectAssociatedPackage(app.packageName))
            },
            onDismiss = { showPackagePicker = false },
        )
    }
}
