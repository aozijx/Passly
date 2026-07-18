package com.aozijx.passly.feature.main.ui

import android.view.WindowManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aozijx.passly.core.message.AppMessageCategory
import com.aozijx.passly.core.message.AppMessageCenter
import com.aozijx.passly.feature.backup.BackupCoordinator
import com.aozijx.passly.feature.backup.components.PlainExportDialog
import com.aozijx.passly.feature.backup.components.PlainExportDialogType
import com.aozijx.passly.feature.main.MainConfigViewModel
import com.aozijx.passly.feature.main.MainSensorController
import com.aozijx.passly.feature.main.MainViewModel
import com.aozijx.passly.feature.main.contract.MainEffect
import com.aozijx.passly.feature.main.contract.MainIntent
import com.aozijx.passly.feature.message.AppMessageHostViewModel
import com.aozijx.passly.feature.verification.VerificationViewModel
import com.aozijx.passly.feature.verification.ui.VerificationScreen
import com.aozijx.passly.ui.theme.AppTheme

@Composable
internal fun MainScreen(
    activity: FragmentActivity,
    viewModel: MainViewModel,
    sensorController: MainSensorController,
    backupCoordinator: BackupCoordinator
) {
    val mainUiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val mainConfigViewModel: MainConfigViewModel = hiltViewModel()
    val mainConfig by mainConfigViewModel.config.collectAsStateWithLifecycle()

    val verificationViewModel: VerificationViewModel = hiltViewModel()
    val messageHostViewModel: AppMessageHostViewModel = hiltViewModel()

    LaunchedEffect(messageHostViewModel) {
        messageHostViewModel.toastMessages.collect { message ->
            Toast.makeText(
                context,
                message.text,
                if (message.longDuration) Toast.LENGTH_LONG else Toast.LENGTH_SHORT
            ).show()
        }
    }

    LaunchedEffect(backupCoordinator.backupMessage) {
        backupCoordinator.backupMessage?.let {
            AppMessageCenter.publish(it, longDuration = true)
            backupCoordinator.clearBackupMessage()
        }
    }

    val plainExportPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let { backupCoordinator.exportPlainBackupToUri(it) }
    }

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is MainEffect.ShowToast -> AppMessageCenter.publish(effect.message)

                is MainEffect.ShowError -> AppMessageCenter.publish(
                    effect.error,
                    longDuration = true
                )

                is MainEffect.ShowPlainExportPicker -> plainExportPickerLauncher.launch(
                    effect.fileName
                )

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
                    PlainExportDialog(
                        type = PlainExportDialogType.DatabaseError,
                        onExportBackup = {
                            viewModel.handleIntent(MainIntent.ExportEmergencyBackup(context))
                        },
                        onResetOrCancel = {
                            AppMessageCenter.publish(
                                text = "应用即将关闭",
                                category = AppMessageCategory.APP_CLOSE
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
                        activity = activity,
                        mainViewModel = viewModel,
                        backupCoordinator = backupCoordinator,
                        onPlainExportPickerRequest = { fileName ->
                            plainExportPickerLauncher.launch(fileName)
                        })
                }

                else -> {
                    VerificationScreen(
                        viewModel = verificationViewModel,
                        activity = activity
                    )
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
