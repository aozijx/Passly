package com.aozijx.passly.feature.vault.presentation.editor.password

import com.aozijx.passly.presentation.shared.components.AppPackagePickerItemUiModel

data class AddPasswordFormState(
    val title: String = "",
    val username: String = "",
    val password: String = "",
    val websiteUrl: String = "",
    val notes: String = "",
    val tags: String = "",
    val associatedAppPackage: String? = null,
    val isPasswordVisible: Boolean = false
) {
    val isValid: Boolean
        get() = title.isNotBlank() && password.isNotBlank()
}

data class AddPasswordUiState(
    val form: AddPasswordFormState = AddPasswordFormState(),
    val availableApps: List<AppPackagePickerItemUiModel> = emptyList(),
    val availableAppsLoaded: Boolean = false,
    val savePhase: AddPasswordSavePhase = AddPasswordSavePhase.Idle,
) {
    val canSave: Boolean
        get() = form.isValid && savePhase == AddPasswordSavePhase.Idle
}

sealed interface AddPasswordSavePhase {
    data object Idle : AddPasswordSavePhase
    data object Saving : AddPasswordSavePhase
}
