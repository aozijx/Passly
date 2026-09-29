package com.aozijx.passly.presentation.feature.settings.main.navigation.core

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aozijx.passly.R
import com.aozijx.passly.presentation.feature.settings.security.RecoveryCodeSettingsEffect
import com.aozijx.passly.presentation.feature.settings.security.RecoveryCodeSettingsViewModel
import com.aozijx.passly.presentation.feature.settings.ui.security.RecoveryCodeScreen

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
