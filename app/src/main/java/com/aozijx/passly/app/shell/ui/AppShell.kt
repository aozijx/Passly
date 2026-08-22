package com.aozijx.passly.app.shell.ui

import android.view.WindowManager
import android.widget.Toast
import androidx.compose.animation.Crossfade
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.FragmentActivity
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aozijx.passly.app.message.model.NoticeCode
import com.aozijx.passly.app.message.model.newAppNotice
import com.aozijx.passly.app.message.presentation.AppNoticeHostViewModel
import com.aozijx.passly.app.shell.AppShellSettingsViewModel
import com.aozijx.passly.app.shell.AppShellViewModel
import com.aozijx.passly.app.shell.FlipToLockSensorController
import com.aozijx.passly.app.shell.contract.AppShellEffect
import com.aozijx.passly.app.shell.contract.AppShellUiAction
import com.aozijx.passly.core.message.compose.LocalAppNoticePublisher
import com.aozijx.passly.core.ui.components.DatabaseRecoveryDialog
import com.aozijx.passly.feature.auth.presentation.BootstrapViewModel
import com.aozijx.passly.feature.auth.presentation.UnlockViewModel
import com.aozijx.passly.feature.auth.ui.AuthenticationScreen
import com.aozijx.passly.feature.recovery.RecoveryModeScreen
import com.aozijx.passly.feature.recovery.RecoveryModeViewModel

@Composable
internal fun AppShell(
    activity: FragmentActivity,
    viewModel: AppShellViewModel,
    sensorController: FlipToLockSensorController
) {
    val mainUiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val noticePublisher = LocalAppNoticePublisher.current

    fun showLocalMessage(text: String, longDuration: Boolean = false) {
        Toast.makeText(
            context,
            text,
            if (longDuration) Toast.LENGTH_LONG else Toast.LENGTH_SHORT
        ).show()
    }

    val mainConfigViewModel: AppShellSettingsViewModel = hiltViewModel()
    val mainConfig by mainConfigViewModel.config.collectAsStateWithLifecycle()

    val unlockViewModel: UnlockViewModel = hiltViewModel()
    val bootstrapViewModel: BootstrapViewModel = hiltViewModel()
    val recoveryViewModel: RecoveryModeViewModel = hiltViewModel()
    val messageHostViewModel: AppNoticeHostViewModel = hiltViewModel()

    LaunchedEffect(messageHostViewModel) {
        messageHostViewModel.toastMessages.collect { message ->
            Toast.makeText(
                context,
                message.text,
                if (message.longDuration) Toast.LENGTH_LONG else Toast.LENGTH_SHORT
            ).show()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is AppShellEffect.ShowToast -> showLocalMessage(effect.message)

                is AppShellEffect.ShowError ->
                    showLocalMessage(effect.error, longDuration = true)

                AppShellEffect.LockedByTimeout, AppShellEffect.NavigateToVault -> Unit
            }
        }
    }

    Crossfade(
        targetState = when {
            mainUiState.databaseError != null -> "error"
            mainUiState.isAuthorized -> "main"
            mainUiState.isRecoveryMode -> "recovery"
            else -> "verification"
        },
        animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec(),
        label = "auth_transition"
    ) { state ->
        when (state) {
            "error" -> {
                DatabaseRecoveryDialog(
                    isBusy = mainUiState.isDatabaseInitializing,
                    onRetry = {
                        viewModel.onAction(AppShellUiAction.RetryDatabaseInitialization)
                    },
                    onRecoverDatabase = {
                        viewModel.onAction(AppShellUiAction.RecoverDatabase)
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

            "main" -> {
                AppShellContent(
                    appShellViewModel = viewModel
                )
            }

            "recovery" -> {
                RecoveryModeScreen(
                    viewModel = recoveryViewModel,
                    onExit = { viewModel.onAction(AppShellUiAction.ExitRecovery) }
                )
            }

            else -> {
                AuthenticationScreen(
                    unlockViewModel = unlockViewModel,
                    bootstrapViewModel = bootstrapViewModel
                )
            }
        }
    }

    val window = activity.window

    SideEffect {
        if (mainConfig.isSecureContentEnabled) {
            window.setFlags(
                WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE
            )
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }

    DisposableEffect(sensorController, mainConfig.isFlipToLockEnabled) {
        sensorController.isFlipLockEnabled = mainConfig.isFlipToLockEnabled
        if (mainConfig.isFlipToLockEnabled) sensorController.register() else sensorController.unregister()
        onDispose {
            sensorController.unregister()
        }
    }

    SideEffect {
        sensorController.isFlipExitAndClearStackEnabled = mainConfig.isFlipExitAndClearStackEnabled
    }

    SideEffect {
        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        insetsController.systemBarsBehavior = if (mainConfig.isStatusBarAutoHide) {
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        } else {
            WindowInsetsControllerCompat.BEHAVIOR_DEFAULT
        }
    }
}
