package com.aozijx.passly.feature.vault.presentation.trash

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.ViewModelStoreOwner
import com.aozijx.passly.feature.vault.presentation.trash.ui.TrashBottomSheet
import com.aozijx.passly.feature.vault.presentation.trash.model.TrashBottomSheetUiState
import com.aozijx.passly.feature.vault.presentation.trash.model.TrashErrorUiState
import com.aozijx.passly.feature.vault.presentation.trash.model.TrashEntryListUiState

@Composable
internal fun TrashRoute(
    viewModelStoreOwner: ViewModelStoreOwner,
    onDismiss: () -> Unit,
) {
    val viewModel = hiltViewModel<TrashViewModel>(viewModelStoreOwner)
    DisposableEffect(viewModel) {
        viewModel.onSheetOpened()
        onDispose(viewModel::onSheetClosed)
    }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val operation = state.operation
    val content = state.content
    val activeEntryId = (operation as? TrashOperationState.Entry)?.entryId
    val error =
        when (operation) {
            is TrashOperationState.Failed -> TrashErrorUiState.OperationFailed(stringResource(operation.message.resourceId))
            TrashOperationState.Emptying,
            is TrashOperationState.Entry,
            TrashOperationState.Idle,
            -> TrashErrorUiState.Hidden
        }
    val contentUiState =
        if (operation == TrashOperationState.Emptying) {
            TrashEntryListUiState.Emptying
        } else {
            when (content) {
                TrashContentState.Loading -> TrashEntryListUiState.Loading
                is TrashContentState.Failed -> TrashEntryListUiState.LoadFailed(stringResource(content.message.resourceId))
                is TrashContentState.Loaded ->
                    TrashUiMapper.map(content.entries)
                        .takeIf { it.isNotEmpty() }
                        ?.let { TrashEntryListUiState.Items(it, activeEntryId) }
                        ?: TrashEntryListUiState.Empty
            }
        }
    TrashBottomSheet(
        state =
            TrashBottomSheetUiState(
                entries = contentUiState,
                isBusy = state.isBusy,
                error = error,
            ),
        onDismiss = onDismiss,
        onRestore = { viewModel.onAction(TrashUiAction.Restore(it.id, it.version)) },
        onDelete = { viewModel.onAction(TrashUiAction.DeletePermanently(it.id, it.version)) },
        onEmpty = { viewModel.onAction(TrashUiAction.Empty) },
        onClearError = { viewModel.onAction(TrashUiAction.ClearError) },
        onRetryLoad = { viewModel.onAction(TrashUiAction.RetryLoad) },
    )
}
