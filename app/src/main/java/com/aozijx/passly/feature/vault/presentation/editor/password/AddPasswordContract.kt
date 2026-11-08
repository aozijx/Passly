package com.aozijx.passly.feature.vault.presentation.editor.password

import com.aozijx.passly.feature.vault.presentation.common.AppPackagePickerItemUiModel
import com.aozijx.passly.feature.vault.presentation.common.AppPackagePickerUiState
import com.aozijx.passly.feature.vault.presentation.editor.EditorLoadPhase

data class AddPasswordFormState(
    val title: String = "",
    val username: String = "",
    val password: String = "",
    val websiteUrl: String = "",
    val notes: String = "",
    val tags: String = "",
    val associatedAppPackage: String? = null,
    val preservedAssociatedAppPackages: Set<String> = emptySet(),
    val isPasswordVisible: Boolean = false,
) {
    val isValid: Boolean
        get() = title.isNotBlank() && password.isNotBlank()
}

data class AddPasswordUiState(
    val form: AddPasswordFormState = AddPasswordFormState(),
    val availableApps: AppPackagePickerUiState<AppPackagePickerItemUiModel> = AppPackagePickerUiState.NotRequested,
    val isSaving: Boolean = false,
    val loadPhase: EditorLoadPhase = EditorLoadPhase.NotRequested,
) {
    val associatedApp: AppPackagePickerItemUiModel?
        get() {
            val apps =
                when (val current = availableApps) {
                    is AppPackagePickerUiState.Matches -> current.installedApps
                    is AppPackagePickerUiState.NoMatches -> current.installedApps
                    AppPackagePickerUiState.NotRequested,
                    AppPackagePickerUiState.Loading,
                    AppPackagePickerUiState.Failed,
                    is AppPackagePickerUiState.NoApps,
                    -> emptyList()
                }
            return form.associatedAppPackage?.let { packageName ->
                apps.firstOrNull { it.packageName == packageName }
            }
        }

    val canSave: Boolean
        get() =
            form.isValid && !isSaving && loadPhase.allowsSaving
}
