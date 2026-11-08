package com.aozijx.passly.feature.recovery.presentation

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aozijx.passly.R
import com.aozijx.passly.feature.recovery.presentation.ui.RecoveryModeScreen
import com.aozijx.passly.presentation.shared.event.collectPageEffects

@Composable
fun RecoveryModeRoute(onExit: () -> Unit) {
    val viewModel: RecoveryModeViewModel = hiltViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(viewModel) {
        val owner = viewModel.onPageEntered()
        onDispose { viewModel.onPageExited(owner) }
    }
    LaunchedEffect(viewModel, lifecycle) {
        collectPageEffects(lifecycle, viewModel.effect) { effect ->
            when (effect) {
                RecoveryModeEffect.ExitRecovery -> {
                    onExit()
                }

                RecoveryModeEffect.PasswordResetCompleted -> {
                    Toast
                        .makeText(
                            context,
                            R.string.recovery_mode_password_reset_success,
                            Toast.LENGTH_LONG,
                        ).show()
                }
            }
        }
    }
    RecoveryModeScreen(
        state = state,
        onSetPassword = { viewModel.onAction(RecoveryModeUiAction.SetPasswordClicked) },
        onExit = { viewModel.onAction(RecoveryModeUiAction.ExitClicked) },
        onNewPasswordChange = { viewModel.onAction(RecoveryModeUiAction.NewPasswordChanged(it)) },
        onConfirmPasswordChange = { viewModel.onAction(RecoveryModeUiAction.ConfirmPasswordChanged(it)) },
        onConfirmPassword = { viewModel.onAction(RecoveryModeUiAction.SubmitNewPassword) },
        onDismissPassword = { viewModel.onAction(RecoveryModeUiAction.DismissPasswordDialog) },
    )
}
