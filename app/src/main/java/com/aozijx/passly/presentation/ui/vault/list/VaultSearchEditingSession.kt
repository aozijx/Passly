package com.aozijx.passly.presentation.ui.vault.list

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

internal class VaultSearchEditingSession {
    var isEditing by mutableStateOf(false)
        private set

    fun updateEditing(editing: Boolean) {
        isEditing = editing
    }

    fun onScreenPaused() {
        isEditing = false
    }
}
