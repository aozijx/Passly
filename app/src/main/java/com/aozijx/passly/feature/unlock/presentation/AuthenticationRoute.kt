package com.aozijx.passly.feature.unlock.presentation

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aozijx.passly.feature.unlock.presentation.ui.AuthenticationScreen

@Composable
fun AuthenticationRoute() {
    val viewModel: UnlockViewModel = hiltViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    BackHandler(enabled = state.passwordSetup == UnlockPasswordSetup.Closed) {
        viewModel.onAction(UnlockUiAction.BackPressed)
    }

    AuthenticationScreen(
        state = state,
        onAction = viewModel::onAction,
    )
}
