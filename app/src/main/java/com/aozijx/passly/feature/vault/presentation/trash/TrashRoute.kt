package com.aozijx.passly.feature.vault.presentation.trash

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.ViewModelStoreOwner
import com.aozijx.passly.feature.vault.presentation.trash.ui.TrashBottomSheet

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
    TrashBottomSheet(
        state = TrashUiMapper.map(state),
        onDismiss = onDismiss,
        onRestore = { viewModel.onAction(TrashUiAction.Restore(it.id, it.version)) },
        onDelete = { viewModel.onAction(TrashUiAction.DeletePermanently(it.id, it.version)) },
        onEmpty = { viewModel.onAction(TrashUiAction.Empty) },
        onClearError = { viewModel.onAction(TrashUiAction.ClearError) },
        onRetryLoad = { viewModel.onAction(TrashUiAction.RetryLoad) },
    )
}
