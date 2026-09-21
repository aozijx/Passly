package com.aozijx.passly.presentation.feature.vault.trash

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aozijx.passly.presentation.feature.vault.list.ui.trash.TrashBottomSheet

@Composable
internal fun TrashRoute(
    onDismiss: () -> Unit,
    viewModel: TrashViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    TrashBottomSheet(
        visible = true,
        entries = state.toUiModels(),
        isLoading = state.isLoading,
        activeEntryId = state.activeEntryId,
        isEmptying = state.isEmptying,
        error = state.error,
        onDismiss = onDismiss,
        onRestore = { viewModel.onAction(TrashUiAction.Restore(it.id, it.version)) },
        onDelete = { viewModel.onAction(TrashUiAction.DeletePermanently(it.id, it.version)) },
        onEmpty = { viewModel.onAction(TrashUiAction.Empty) },
        onClearError = { viewModel.onAction(TrashUiAction.ClearError) },
    )
}
