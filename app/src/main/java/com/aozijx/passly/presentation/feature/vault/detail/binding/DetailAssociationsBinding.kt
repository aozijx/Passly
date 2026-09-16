package com.aozijx.passly.presentation.feature.vault.detail.binding

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.aozijx.passly.core.platform.packageinfo.rememberInstalledAppIconBitmap
import com.aozijx.passly.core.platform.packageinfo.InstalledAppServicesProvider
import com.aozijx.passly.domain.entry.model.Entry
import com.aozijx.passly.presentation.feature.vault.detail.DetailEditKey
import com.aozijx.passly.presentation.feature.vault.detail.DetailUiAction
import com.aozijx.passly.presentation.feature.vault.detail.DetailUiState
import com.aozijx.passly.presentation.ui.shared.components.AppPackagePickerBottomSheet
import com.aozijx.passly.presentation.ui.shared.components.AppPackagePickerItemUiModel
import com.aozijx.passly.presentation.ui.vault.detail.component.AssociatedInfoSection
import com.aozijx.passly.presentation.ui.vault.detail.model.DetailAssociatedInfoUiModel
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
internal fun DetailAssociationsBinding(
    entry: Entry,
    uiState: DetailUiState,
    onAction: (DetailUiAction) -> Unit,
) {
    val context = LocalContext.current
    val services = remember {
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            InstalledAppServicesProvider::class.java,
        )
    }
    val appCatalog = remember(services) { services.getInstalledAppCatalog() }
    val applicationIds = remember(entry.associations.applicationIds) {
        entry.associations.applicationIds.sorted()
    }
    val associatedApps by produceState(
        initialValue = applicationIds.map(::placeholderApp),
        applicationIds,
        appCatalog,
    ) {
        value = applicationIds.map(::placeholderApp)
        value = withContext(Dispatchers.IO) {
            applicationIds.map { packageName ->
                val metadata = appCatalog.getAppMetadata(packageName)
                AppPackagePickerItemUiModel(
                    label = metadata?.label?.takeIf(String::isNotBlank) ?: packageName,
                    packageName = packageName,
                )
            }
        }
    }
    var showPackagePicker by remember { mutableStateOf(false) }
    val pickerApps by produceState(
        initialValue = emptyList(),
        showPackagePicker,
        appCatalog,
    ) {
        value = if (showPackagePicker) {
            value = emptyList()
            withContext(Dispatchers.IO) {
                appCatalog.getLaunchableApps().map { metadata ->
                    AppPackagePickerItemUiModel(
                        label = metadata.label,
                        packageName = metadata.packageName,
                    )
                }
            }
        } else {
            emptyList()
        }
    }

    AssociatedInfoSection(
        model = DetailAssociatedInfoUiModel(
            domain = entry.associatedDomain,
            editedDomain = uiState.fieldEdits.draft(DetailEditKey.DOMAIN),
            isEditingDomain = uiState.fieldEdits.isEditing(DetailEditKey.DOMAIN),
        ),
        associatedApps = associatedApps,
        appIcon = { packageName -> rememberInstalledAppIconBitmap(packageName) },
        onDomainEditStarted = { onAction(DetailUiAction.StartDomainEdit) },
        onDomainChanged = { onAction(DetailUiAction.UpdateDomainDraft(it)) },
        onDomainSaved = { onAction(DetailUiAction.SaveDomain) },
        onPackagePickerRequested = { showPackagePicker = true },
    )

    if (showPackagePicker) {
        AppPackagePickerBottomSheet(
            apps = pickerApps,
            appIcon = { packageName -> rememberInstalledAppIconBitmap(packageName) },
            onSelect = { app ->
                showPackagePicker = false
                onAction(DetailUiAction.SelectAssociatedPackage(app.packageName))
            },
            onDismiss = { showPackagePicker = false },
        )
    }
}

private fun placeholderApp(packageName: String) = AppPackagePickerItemUiModel(
    label = packageName,
    packageName = packageName,
)
