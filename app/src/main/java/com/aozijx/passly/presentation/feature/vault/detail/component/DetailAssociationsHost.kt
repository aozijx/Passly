package com.aozijx.passly.presentation.feature.vault.detail.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.aozijx.passly.app.platform.packageinfo.rememberInstalledAppIconBitmap
import com.aozijx.passly.core.platform.packageinfo.InstalledAppServicesProvider
import com.aozijx.passly.domain.entry.model.Entry
import com.aozijx.passly.presentation.feature.vault.detail.DetailEditCompletion
import com.aozijx.passly.feature.vault.detail.DetailEntryPatch
import com.aozijx.passly.presentation.feature.vault.detail.DetailUiAction
import com.aozijx.passly.presentation.feature.vault.detail.EntryEditState
import com.aozijx.passly.presentation.ui.shared.components.AppPackagePickerBottomSheet
import com.aozijx.passly.presentation.ui.shared.components.AppPackagePickerItemUiModel
import com.aozijx.passly.presentation.ui.vault.detail.component.AssociatedInfoSection
import com.aozijx.passly.presentation.ui.vault.detail.model.DetailAssociatedInfoUiModel
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
internal fun DetailAssociationsHost(
    entry: Entry,
    editState: EntryEditState,
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
            isEditingDomain = editState.isEditingDomain,
        ),
        associatedApps = associatedApps,
        appIcon = { packageName -> rememberInstalledAppIconBitmap(packageName) },
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
        onPackagePickerRequested = { showPackagePicker = true },
    )

    if (showPackagePicker) {
        AppPackagePickerBottomSheet(
            apps = pickerApps,
            appIcon = { packageName -> rememberInstalledAppIconBitmap(packageName) },
            onSelect = { app ->
                showPackagePicker = false
                editState.editedPackage = app.packageName
                onAction(
                    DetailUiAction.CommitPatch(
                        DetailEntryPatch.Associations(
                            primaryUrl = entry.associations.primaryUrl,
                            applicationIds = setOf(app.packageName),
                        ),
                        DetailEditCompletion.Associations,
                    ),
                )
            },
            onDismiss = { showPackagePicker = false },
        )
    }
}

private fun placeholderApp(packageName: String) = AppPackagePickerItemUiModel(
    label = packageName,
    packageName = packageName,
)
