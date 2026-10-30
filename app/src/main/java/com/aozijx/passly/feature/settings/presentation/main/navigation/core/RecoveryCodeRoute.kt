package com.aozijx.passly.feature.settings.presentation.main.navigation.core

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aozijx.passly.R
import com.aozijx.passly.feature.settings.presentation.security.RecoveryCodeSettingsEffect
import com.aozijx.passly.feature.settings.presentation.security.RecoveryCodeSettingsViewModel
import com.aozijx.passly.feature.settings.presentation.ui.security.RecoveryCodeScreen

@Composable
internal fun RecoveryCodeRoute(
    onBack: (() -> Unit)?,
) {
    val viewModel = hiltViewModel<RecoveryCodeSettingsViewModel>()
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val copySuccessMessage = stringResource(
        R.string.field_copy_success_message,
        stringResource(R.string.recovery_code_label),
    )

    DisposableEffect(viewModel) {
        val owner = viewModel.onPageEntered()
        onDispose { viewModel.onPageExited(owner) }
    }

    LaunchedEffect(viewModel, context, copySuccessMessage) {
        viewModel.effects.collect { effect ->
            when (effect) {
                RecoveryCodeSettingsEffect.Copied -> Toast.makeText(
                    context,
                    copySuccessMessage,
                    Toast.LENGTH_SHORT,
                ).show()
            }
        }
    }

    RecoveryCodeScreen(
        state = state,
        onAction = viewModel::onAction,
        onBack = onBack,
    )
}
