package com.aozijx.passly.presentation.feature.database.reset

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aozijx.passly.presentation.feature.database.reset.ui.DatabaseResetSheet

@Composable
internal fun DatabaseResetRoute(
    onDismiss: () -> Unit,
) {
    val viewModel: DatabaseResetViewModel = hiltViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    DatabaseResetSheet(
        state = state,
        onDismiss = onDismiss,
        onReset = { viewModel.onAction(DatabaseResetUiAction.Reset) },
    )
}
