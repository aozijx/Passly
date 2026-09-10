package com.aozijx.passly.presentation.feature.database.reset

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aozijx.passly.presentation.ui.database.reset.DatabaseResetSheet
import com.aozijx.passly.presentation.ui.database.reset.model.DatabaseResetEventHandler
import com.aozijx.passly.presentation.ui.database.reset.model.DatabaseResetSheetState

@Composable
internal fun DatabaseResetOverlay(
    onDismiss: () -> Unit,
) {
    val viewModel: DatabaseResetViewModel = hiltViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    DatabaseResetSheet(
        state = DatabaseResetSheetState(
            isResetting = state.isResetting,
            isResetComplete = state.isResetComplete,
            error = state.error,
        ),
        eventHandler = object : DatabaseResetEventHandler {
            override fun onDismiss() = onDismiss.invoke()
            override fun onReset() = viewModel.onAction(DatabaseResetUiAction.Reset)
        },
    )
}
