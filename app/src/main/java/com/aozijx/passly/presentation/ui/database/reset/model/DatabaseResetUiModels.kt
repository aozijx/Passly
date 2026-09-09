package com.aozijx.passly.presentation.ui.database.reset.model

internal data class DatabaseResetSheetState(
    val isResetting: Boolean = false,
    val isResetComplete: Boolean = false,
    val error: String? = null,
)

internal interface DatabaseResetEventHandler {
    fun onDismiss()
    fun onReset()
}