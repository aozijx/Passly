package com.aozijx.passly.feature.vault.presentation.trash

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aozijx.passly.feature.vault.presentation.trash.ui.TrashBottomSheet

@Composable
internal fun TrashRoute(
    onDismiss: () -> Unit,
) {
    val viewModel = hiltViewModel<TrashViewModel>()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val operation = state.operation
    val content = state.content
    val activeEntryId = (operation as? TrashOperationState.Entry)?.entryId
    val error = when {
        operation is TrashOperationState.Failed -> operation.message
        content is TrashContentState.Failed -> content.message
        else -> null
    }
    TrashBottomSheet(
        visible = true,
        entries = content.toUiModels(),
        isLoading = content == TrashContentState.Loading,
        activeEntryId = activeEntryId,
        isEmptying = operation == TrashOperationState.Emptying,
        error = error,
        onDismiss = onDismiss,
        onRestore = { viewModel.onAction(TrashUiAction.Restore(it.id, it.version)) },
        onDelete = { viewModel.onAction(TrashUiAction.DeletePermanently(it.id, it.version)) },
        onEmpty = { viewModel.onAction(TrashUiAction.Empty) },
        onClearError = { viewModel.onAction(TrashUiAction.ClearError) },
    )
}
