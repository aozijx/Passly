package com.aozijx.passly.presentation.feature.unlock

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aozijx.passly.presentation.feature.unlock.ui.AuthenticationScreen

@Composable
fun AuthenticationRoute() {
    val viewModel: UnlockViewModel = hiltViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    BackHandler(enabled = !state.showSetPasswordDialog) {
        viewModel.onAction(UnlockUiAction.BackPressed)
    }

    AuthenticationScreen(
        state = state,
        onAction = viewModel::onAction,
    )
}
