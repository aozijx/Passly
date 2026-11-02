package com.aozijx.passly.feature.database.presentation.reset

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aozijx.passly.feature.database.presentation.reset.ui.DatabaseResetSheet

@Composable
internal fun DatabaseResetRoute(onDismiss: () -> Unit) {
    val viewModel: DatabaseResetViewModel = hiltViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    DisposableEffect(viewModel) {
        val owner = viewModel.onPageEntered()
        onDispose { viewModel.onPageExited(owner) }
    }

    DatabaseResetSheet(
        state = state,
        onDismiss = onDismiss,
        onReset = { viewModel.onAction(DatabaseResetUiAction.Reset) },
    )
}
