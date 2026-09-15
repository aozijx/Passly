package com.aozijx.passly.presentation.feature.shell

import android.widget.Toast
import androidx.compose.animation.Crossfade
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.fragment.app.FragmentActivity
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aozijx.passly.app.message.model.NoticeCode
import com.aozijx.passly.app.message.model.newAppNotice
import com.aozijx.passly.app.message.presentation.AppNoticeHostViewModel
import com.aozijx.passly.app.shell.FlipToLockSensorController
import com.aozijx.passly.app.message.compose.LocalAppNoticePublisher
import com.aozijx.passly.presentation.ui.shell.DatabaseErrorDialog
import com.aozijx.passly.presentation.feature.onboarding.BootstrapViewModel
import com.aozijx.passly.presentation.feature.unlock.UnlockViewModel
import com.aozijx.passly.presentation.feature.unlock.AuthenticationScreen
import com.aozijx.passly.presentation.feature.recovery.RecoveryModeScreen
import com.aozijx.passly.presentation.feature.recovery.RecoveryModeViewModel

@Composable
internal fun AppShell(
    activity: FragmentActivity,
    viewModel: AppShellViewModel,
    sensorController: FlipToLockSensorController
) {
    val mainUiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val noticePublisher = LocalAppNoticePublisher.current

    val mainConfigViewModel: AppShellSettingsViewModel = hiltViewModel()
    val mainConfig by mainConfigViewModel.config.collectAsStateWithLifecycle()

    val unlockViewModel: UnlockViewModel = hiltViewModel()
    val bootstrapViewModel: BootstrapViewModel = hiltViewModel()
    val recoveryViewModel: RecoveryModeViewModel = hiltViewModel()
    val messageHostViewModel: AppNoticeHostViewModel = hiltViewModel()

    LaunchedEffect(messageHostViewModel, context) {
        messageHostViewModel.toastMessages.collect { message ->
            Toast.makeText(
                context,
                message.text,
                if (message.longDuration) Toast.LENGTH_LONG else Toast.LENGTH_SHORT
            ).show()
        }
    }

    LaunchedEffect(viewModel, context) {
        viewModel.effects.collect { effect ->
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
        targetState = mainUiState.destination,
        animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec(),
        label = "auth_transition"
    ) { state ->
        when (state) {
            AppShellDestination.DATABASE_ERROR -> {
                DatabaseErrorDialog(
                    isBusy = mainUiState.isDatabaseInitializing,
                    onRetry = {
                        viewModel.onAction(AppShellUiAction.RetryDatabaseInitialization)
                    },
                    onCloseApp = {
                        noticePublisher.publish(
                            newAppNotice(NoticeCode.APP_CLOSE_REMINDER)
                        )
                        activity.window.decorView.postDelayed(
                            { activity.finishAffinity() },
                            1_000L
                        )
                    }
                )
            }

            AppShellDestination.VAULT -> {
                AppShellContent(
                    appShellViewModel = viewModel
                )
            }

            AppShellDestination.RECOVERY -> {
                RecoveryModeScreen(
                    viewModel = recoveryViewModel,
                    onExit = { viewModel.onAction(AppShellUiAction.ExitRecovery) }
                )
            }

            AppShellDestination.AUTHENTICATION -> {
                AuthenticationScreen(
                    unlockViewModel = unlockViewModel,
                    bootstrapViewModel = bootstrapViewModel
                )
            }
        }
    }

    AppWindowPolicyEffects(
        window = activity.window,
        settings = mainConfig,
        sensorController = sensorController,
    )
}
