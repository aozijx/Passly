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
import androidx.compose.ui.res.stringResource
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aozijx.passly.R
import com.aozijx.passly.core.error.ui.toUiMessage
import com.aozijx.passly.core.message.AppMessageCategory
import com.aozijx.passly.core.message.AppMessageCenter
import com.aozijx.passly.core.ui.components.PlainExportDialog
import com.aozijx.passly.core.ui.components.PlainExportDialogType
import com.aozijx.passly.core.ui.theme.AppTheme
import com.aozijx.passly.feature.auth.presentation.AuthenticationViewModel
import com.aozijx.passly.feature.auth.ui.AuthenticationScreen
import com.aozijx.passly.feature.backup.BackupViewModel
import com.aozijx.passly.feature.backup.contract.BackupEffect
import com.aozijx.passly.feature.backup.contract.BackupIntent
import com.aozijx.passly.feature.backup.contract.BackupOperationStatus
import com.aozijx.passly.feature.main.MainConfigViewModel
import com.aozijx.passly.feature.main.MainSensorController
import com.aozijx.passly.feature.main.MainViewModel
import com.aozijx.passly.feature.main.contract.MainEffect
import com.aozijx.passly.feature.main.contract.MainIntent
import com.aozijx.passly.feature.message.AppMessageHostViewModel

@Composable
internal fun MainScreen(
    activity: FragmentActivity,
    viewModel: MainViewModel,
    sensorController: MainSensorController
) {
    val mainUiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val mainConfigViewModel: MainConfigViewModel = hiltViewModel()
    val mainConfig by mainConfigViewModel.config.collectAsStateWithLifecycle()

    val backupViewModel: BackupViewModel = hiltViewModel()
    val backupState by backupViewModel.uiState.collectAsStateWithLifecycle()

    val authenticationViewModel: AuthenticationViewModel = hiltViewModel()
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

    val exportSuccessMsg = stringResource(R.string.backup_export_success)
    val importSuccessMsg = stringResource(R.string.backup_import_success)
    val permOkMsg = stringResource(R.string.backup_directory_permission_ok)
    val plainExportSuccessMsg = stringResource(R.string.backup_plain_export_success)
    val unknownErrorMsg = stringResource(R.string.backup_error_unknown)

    val plainExportPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let { backupViewModel.onIntent(BackupIntent.ExportPlainBackupToUri(it)) }
    }

    // --- 收集 BackupEffect ---
    LaunchedEffect(backupViewModel) {
        backupViewModel.effect.collect { effect ->
            when (effect) {
                is BackupEffect.ShowError -> {
                    val msg = effect.error.toUiMessage(unknownErrorMsg)
                    AppMessageCenter.publish(msg, longDuration = true)
                }

                is BackupEffect.ShowPlainExportPicker -> plainExportPickerLauncher.launch(effect.fileName)
                BackupEffect.RequestAuth -> {
                    viewModel.requestAuth(
                        onSuccess = { backupViewModel.onIntent(BackupIntent.ExecuteBackup) }
                    )
                }
            }
        }
    }

    LaunchedEffect(backupState.status) {
        when (val status = backupState.status) {
            is BackupOperationStatus.Success -> {
                val msg = when (status.type) {
                    BackupOperationStatus.OperationType.EXPORT -> exportSuccessMsg
                    BackupOperationStatus.OperationType.IMPORT -> importSuccessMsg
                    BackupOperationStatus.OperationType.PLAIN_EXPORT -> plainExportSuccessMsg
                    BackupOperationStatus.OperationType.PERMISSION_CHECK -> permOkMsg
                }
                AppMessageCenter.publish(msg, longDuration = true)
                backupViewModel.onIntent(BackupIntent.ResetBackupStatus)
            }

            is BackupOperationStatus.Failure -> {
                val errorMsg = backupState.error?.toUiMessage(unknownErrorMsg) ?: unknownErrorMsg
                AppMessageCenter.publish(errorMsg, longDuration = true)
                backupViewModel.onIntent(BackupIntent.ResetBackupStatus)
            }

            else -> Unit
        }
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
                            viewModel.handleIntent(MainIntent.RetryDatabaseInitialization)
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
                        mainViewModel = viewModel,
                        backupViewModel = backupViewModel
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
