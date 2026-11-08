package com.aozijx.passly.feature.vault.presentation.editor

sealed interface EditorSaveState {
    data object Idle : EditorSaveState

    data object Saving : EditorSaveState

    val isSaving: Boolean
        get() = this is Saving

    val canStartSave: Boolean
        get() =
            when (this) {
                Idle -> true
                Saving -> false
            }
}
