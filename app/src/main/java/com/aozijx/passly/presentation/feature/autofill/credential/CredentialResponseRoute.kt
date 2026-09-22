package com.aozijx.passly.presentation.feature.autofill.credential

import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
@Composable
internal fun CredentialResponseRoute(
    action: CredentialResponseUiAction,
    viewModel: CredentialResponseViewModel,
    onComplete: (Intent) -> Unit,
    onUnrecoverable: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(action, viewModel) {
        viewModel.onAction(action)
    }
    LaunchedEffect(state) {
        when (val current = state) {
            is CredentialResponseUiState.Complete -> onComplete(current.resultIntent)
            CredentialResponseUiState.Unrecoverable -> onUnrecoverable()
            CredentialResponseUiState.Loading -> Unit
        }
    }
}
