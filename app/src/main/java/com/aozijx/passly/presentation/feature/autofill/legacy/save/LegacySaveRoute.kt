package com.aozijx.passly.presentation.feature.autofill.legacy.save

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
internal fun LegacySaveRoute(
    token: String?,
    viewModel: LegacySaveViewModel,
    onSaved: () -> Unit,
    onUnrecoverable: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(token, viewModel) {
        viewModel.onToken(token)
    }
    LaunchedEffect(state) {
        when (state) {
            LegacySaveUiState.Loading -> Unit
            LegacySaveUiState.Saved -> onSaved()
            LegacySaveUiState.Unrecoverable -> onUnrecoverable()
        }
    }
}
