package com.aozijx.passly.feature.vault.presentation.editor.password

import com.aozijx.passly.feature.vault.presentation.common.AppPackagePickerItemUiModel
import com.aozijx.passly.feature.vault.presentation.common.AppPackagePickerUiState
import com.aozijx.passly.feature.vault.presentation.editor.EditorLoadPhase

sealed interface AssociatedAppSelection {
    data object NotSelected : AssociatedAppSelection

    data class Selected(
        val packageName: String,
    ) : AssociatedAppSelection
}

sealed interface AssociatedAppDisplayState {
    data object NotSelected : AssociatedAppDisplayState

    data class Installed(
        val app: AppPackagePickerItemUiModel,
    ) : AssociatedAppDisplayState

    data class Unresolved(
        val packageName: String,
    ) : AssociatedAppDisplayState
}

data class AddPasswordFormState(
    val title: String = "",
    val username: String = "",
    val password: String = "",
    val websiteUrl: String = "",
    val notes: String = "",
    val tags: String = "",
    val associatedAppSelection: AssociatedAppSelection = AssociatedAppSelection.NotSelected,
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
    val associatedAppDisplayState: AssociatedAppDisplayState
        get() {
            return when (val selection = form.associatedAppSelection) {
                AssociatedAppSelection.NotSelected -> {
                    AssociatedAppDisplayState.NotSelected
                }

                is AssociatedAppSelection.Selected -> {
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
                    apps
                        .firstOrNull { it.packageName == selection.packageName }
                        ?.let(AssociatedAppDisplayState::Installed)
                        ?: AssociatedAppDisplayState.Unresolved(selection.packageName)
                }
            }
        }

    val canSave: Boolean
        get() =
            form.isValid && !isSaving && loadPhase.allowsSaving
}
