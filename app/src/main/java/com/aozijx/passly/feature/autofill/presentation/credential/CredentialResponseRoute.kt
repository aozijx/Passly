package com.aozijx.passly.feature.autofill.presentation.credential

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.flow.collect

@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
@Composable
internal fun CredentialResponseRoute(
    action: CredentialResponseUiAction,
    viewModel: CredentialResponseViewModel,
    onComplete: (CredentialResponseCompletion) -> Unit,
    onUnrecoverable: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(action, viewModel) {
        viewModel.onAction(action)
    }
    LaunchedEffect(viewModel, lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.completions.collect(onComplete)
        }
    }
    LaunchedEffect(state) {
        if (state is CredentialResponseUiState.Unrecoverable) onUnrecoverable()
    }
}
