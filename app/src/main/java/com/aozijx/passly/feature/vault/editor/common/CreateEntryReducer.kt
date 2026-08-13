package com.aozijx.passly.feature.vault.editor.common

internal sealed interface CreateEntryMutation<out Form> {
    data class FormChanged<Form>(
        val form: Form,
        val canSave: Boolean,
    ) : CreateEntryMutation<Form>
    data object SaveStarted : CreateEntryMutation<Nothing>
    data class SaveFailed(val canSave: Boolean) : CreateEntryMutation<Nothing>
}

internal object CreateEntryReducer {
    fun <Form> reduce(
        state: CreateEntryUiState<Form>,
        mutation: CreateEntryMutation<Form>,
    ): CreateEntryUiState<Form> = when (mutation) {
        is CreateEntryMutation.FormChanged -> state.copy(
            form = mutation.form,
            canSave = mutation.canSave,
        )
        CreateEntryMutation.SaveStarted -> state.copy(
            isSaving = true,
            canSave = false,
        )
        is CreateEntryMutation.SaveFailed -> state.copy(
            isSaving = false,
            canSave = mutation.canSave,
        )
    }
}
