package com.aozijx.passly.feature.main.ui

import android.view.WindowManager
import android.widget.Toast
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aozijx.passly.R
import com.aozijx.passly.core.message.compose.LocalAppNoticePublisher
import com.aozijx.passly.core.ui.components.DatabaseRecoveryDialog
import com.aozijx.passly.core.ui.theme.AppTheme
import com.aozijx.passly.domain.notice.model.NoticeCode
import com.aozijx.passly.domain.notice.model.newAppNotice
import com.aozijx.passly.feature.auth.presentation.AuthenticationViewModel
import com.aozijx.passly.feature.auth.ui.AuthenticationScreen
import com.aozijx.passly.feature.main.MainConfigViewModel
import com.aozijx.passly.feature.main.MainSensorController
import com.aozijx.passly.feature.main.MainViewModel
import com.aozijx.passly.feature.main.contract.MainEffect
import com.aozijx.passly.feature.main.contract.MainIntent
import com.aozijx.passly.feature.message.AppNoticeHostViewModel

@Composable
internal fun MainScreen(
    activity: FragmentActivity,
    viewModel: MainViewModel,
    sensorController: MainSensorController
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

    val mainConfigViewModel: MainConfigViewModel = hiltViewModel()
    val mainConfig by mainConfigViewModel.config.collectAsStateWithLifecycle()

    val authenticationViewModel: AuthenticationViewModel = hiltViewModel()
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
                is MainEffect.ShowToast -> showLocalMessage(effect.message)

                is MainEffect.ShowError ->
                    showLocalMessage(effect.error, longDuration = true)

                MainEffect.LockedByTimeout, MainEffect.NavigateToVault -> Unit
            }
        }
    }

    AppTheme(
        darkTheme = mainUiState.isDarkMode,
        dynamicColor = mainUiState.isDynamicColor,
        themeColor = mainUiState.themeColor
    ) {
        Crossfade(
            targetState = when {
                mainUiState.databaseError != null -> "error"
                mainUiState.isAuthorized -> "main"
                else -> "verification"
            },
            animationSpec = tween(300),
            label = "auth_transition"
        ) { state ->
            when (state) {
                "error" -> {
                    DatabaseRecoveryDialog(
                        isBusy = mainUiState.isDatabaseInitializing,
                        onRetry = {
                            viewModel.handleIntent(MainIntent.RetryDatabaseInitialization)
                        },
                        onRecoverDatabase = {
                            viewModel.handleIntent(MainIntent.RecoverDatabase)
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
                    AppMainContent(
                        mainViewModel = viewModel
                    )
                }

                else -> {
                    AuthenticationScreen(viewModel = authenticationViewModel)
                }
            }
        }
    }

    val window = activity.window

    LaunchedEffect(
        mainConfig.isSecureContentEnabled,
        mainConfig.isFlipToLockEnabled,
        mainConfig.isFlipExitAndClearStackEnabled,
        mainConfig.isStatusBarAutoHide
    ) {
        if (mainConfig.isSecureContentEnabled) {
            window.setFlags(
                WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE
            )
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }

        sensorController.isFlipLockEnabled = mainConfig.isFlipToLockEnabled
        if (mainConfig.isFlipToLockEnabled) sensorController.register() else sensorController.unregister()

        sensorController.isFlipExitAndClearStackEnabled = mainConfig.isFlipExitAndClearStackEnabled

        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        insetsController.systemBarsBehavior = if (mainConfig.isStatusBarAutoHide) {
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        } else {
            WindowInsetsControllerCompat.BEHAVIOR_DEFAULT
        }
    }
    DisposableEffect(Unit) { onDispose { sensorController.unregister() } }
}
