package com.aozijx.passly.presentation.feature.shell

import android.view.Window
import android.widget.Toast
import androidx.compose.animation.Crossfade
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aozijx.passly.app.message.compose.LocalAppNoticePublisher
import com.aozijx.passly.app.message.model.NoticeCode
import com.aozijx.passly.app.message.model.newAppNotice
import com.aozijx.passly.app.message.presentation.AppNoticeHostViewModel
import com.aozijx.passly.app.shell.FlipToLockSensorController
import com.aozijx.passly.presentation.feature.onboarding.BootstrapViewModel
import com.aozijx.passly.presentation.feature.recovery.RecoveryModeScreen
import com.aozijx.passly.presentation.feature.recovery.RecoveryModeViewModel
import com.aozijx.passly.presentation.feature.unlock.AuthenticationScreen
import com.aozijx.passly.presentation.feature.unlock.UnlockViewModel
import com.aozijx.passly.presentation.ui.shell.DatabaseErrorDialog
import kotlinx.coroutines.flow.Flow

@Composable
internal fun AppShell(
    window: Window,
    uiState: AppShellUiState,
    effects: Flow<AppShellEffect>,
    onAction: (AppShellUiAction) -> Unit,
    onCloseApp: () -> Unit,
    sensorController: FlipToLockSensorController,
) {
    val context = LocalContext.current
    val noticePublisher = LocalAppNoticePublisher.current
    val settingsViewModel: AppShellSettingsViewModel = hiltViewModel()
    val settings by settingsViewModel.config.collectAsStateWithLifecycle()
    val messageHostViewModel: AppNoticeHostViewModel = hiltViewModel()

    LaunchedEffect(messageHostViewModel, context) {
        messageHostViewModel.toastMessages.collect { message ->
            Toast.makeText(
                context,
                message.text,
                if (message.longDuration) Toast.LENGTH_LONG else Toast.LENGTH_SHORT,
            ).show()
        }
    }

    LaunchedEffect(effects, context) {
        effects.collect { effect ->
            when (effect) {
                is AppShellEffect.ShowError -> Toast.makeText(
                    context,
                    effect.error,
                    Toast.LENGTH_LONG,
                ).show()
            }
        }
    }

    Crossfade(
        targetState = uiState.destination,
        animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec(),
        label = "auth_transition",
    ) { destination ->
        when (destination) {
            AppShellDestination.DATABASE_ERROR -> {
                DatabaseErrorDialog(
                    isBusy = uiState.isDatabaseRetrying,
                    onRetry = { onAction(AppShellUiAction.RetryDatabaseSession) },
                    onCloseApp = {
                        noticePublisher.publish(newAppNotice(NoticeCode.APP_CLOSE_REMINDER))
                        window.decorView.postDelayed(onCloseApp, 1_000L)
                    },
                )
            }

            AppShellDestination.VAULT -> {
                PasslyAppNavigation(
                    onUserInteraction = {
                        onAction(AppShellUiAction.UpdateInteraction)
                    },
                )
            }

            AppShellDestination.RECOVERY -> {
                val recoveryViewModel: RecoveryModeViewModel = hiltViewModel()
                RecoveryModeScreen(
                    viewModel = recoveryViewModel,
                    onExit = { onAction(AppShellUiAction.ExitRecovery) },
                )
            }

            AppShellDestination.AUTHENTICATION -> {
                val unlockViewModel: UnlockViewModel = hiltViewModel()
                val bootstrapViewModel: BootstrapViewModel = hiltViewModel()
                AuthenticationScreen(
                    unlockViewModel = unlockViewModel,
                    bootstrapViewModel = bootstrapViewModel,
                )
            }
        }
    }

    AppWindowPolicyEffects(
        window = window,
        settings = settings,
        sensorController = sensorController,
    )
}
