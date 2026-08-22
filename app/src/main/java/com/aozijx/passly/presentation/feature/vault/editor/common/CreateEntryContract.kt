package com.aozijx.passly.presentation.feature.vault.editor.common

data class CreateEntryUiState<Form>(
    val form: Form,
    val canSave: Boolean,
    val isSaving: Boolean = false
)

sealed interface CreateEntryEffect {
    data object Saved : CreateEntryEffect
    data class SaveFailed(val message: String?) : CreateEntryEffect
}
