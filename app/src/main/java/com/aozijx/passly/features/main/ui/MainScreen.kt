package com.aozijx.passly.features.main.ui

import android.view.WindowManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aozijx.passly.R
import com.aozijx.passly.core.designsystem.LoadingMask
import com.aozijx.passly.core.di.appViewModelFactory
import com.aozijx.passly.core.theme.AppTheme
import com.aozijx.passly.data.local.config.DatabaseConfig
import com.aozijx.passly.features.backup.components.PlainExportDialog
import com.aozijx.passly.features.backup.components.PlainExportDialogType
import com.aozijx.passly.features.main.MainSensorController
import com.aozijx.passly.features.main.MainViewModel
import com.aozijx.passly.features.main.contract.MainEffect
import com.aozijx.passly.features.main.contract.MainIntent
import com.aozijx.passly.features.settings.SettingsViewModel
import com.aozijx.passly.features.verification.VerificationScreen
import com.aozijx.passly.features.verification.VerificationViewModel
import kotlin.system.exitProcess

@Composable
internal fun MainScreen(
    activity: FragmentActivity,
    viewModel: MainViewModel,
    sensorController: MainSensorController
) {
    val mainUiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val settingsViewModel: SettingsViewModel = viewModel(
        factory = appViewModelFactory(activity.application)
    )
    val settingsUiState by settingsViewModel.uiState.collectAsStateWithLifecycle()

    val verificationViewModel: VerificationViewModel = viewModel(
        factory = appViewModelFactory(activity.application)
    )

    LaunchedEffect(settingsViewModel.backup.backupMessage) {
        settingsViewModel.backup.backupMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            settingsViewModel.backup.clearBackupMessage()
        }
    }

    val plainExportPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let { settingsViewModel.backup.exportPlainBackupToUri(it) }
    }

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is MainEffect.ShowToast -> Toast.makeText(
                    activity,
                    effect.message,
                    Toast.LENGTH_SHORT
                ).show()

                is MainEffect.ShowError -> Toast.makeText(
                    activity,
                    effect.error,
                    Toast.LENGTH_LONG
                ).show()

                is MainEffect.ShowPlainExportPicker -> plainExportPickerLauncher.launch(
                    effect.fileName
                )

                MainEffect.LockedByTimeout, MainEffect.NavigateToVault -> Unit
            }
        }
    }

    AppTheme(
        darkTheme = if (mainUiState.isDarkMode == true) true else null,
        dynamicColor = mainUiState.isDynamicColor
    ) {
        when {
            mainUiState.databaseError != null -> {
                PlainExportDialog(
                    type = PlainExportDialogType.DatabaseError,
                    onExportBackup = {
                        viewModel.handleIntent(
                            MainIntent.ExportEmergencyBackup(context)
                        )
                    },
                    onResetOrCancel = {
                        context.deleteDatabase(DatabaseConfig.DATABASE_NAME)
                        Toast.makeText(
                            context,
                            "数据库已清除，请重启应用",
                            Toast.LENGTH_SHORT
                        ).show()
                        activity.finishAffinity()
                        exitProcess(0)
                    }
                )
            }

            mainUiState.isAuthorized && mainUiState.isDatabaseInitializing -> {
                LoadingMask(message = activity.getString(R.string.loading))
            }

            mainUiState.isAuthorized -> {
                AppMainContent(
                    activity = activity,
                    mainViewModel = viewModel,
                    settingsViewModel = settingsViewModel,
                    onPlainExportPickerRequest = { fileName ->
                        plainExportPickerLauncher.launch(fileName)
                    }
                )
            }

            else -> {
                VerificationScreen(
                    viewModel = verificationViewModel,
                    activity = activity,
                    preferPasswordFirst = settingsUiState.isPasswordPreferredAuthFirst
                )
            }
        }
    }

    val window = activity.window

    LaunchedEffect(
        settingsUiState.isSecureContentEnabled,
        settingsUiState.isFlipToLockEnabled,
        settingsUiState.isFlipExitAndClearStackEnabled,
        settingsUiState.isStatusBarAutoHide
    ) {
        if (settingsUiState.isSecureContentEnabled) {
            window.setFlags(
                WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE
            )
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }

        sensorController.isFlipLockEnabled = settingsUiState.isFlipToLockEnabled
        if (settingsUiState.isFlipToLockEnabled) sensorController.register() else sensorController.unregister()

        sensorController.isFlipExitAndClearStackEnabled =
            settingsUiState.isFlipExitAndClearStackEnabled

        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        insetsController.systemBarsBehavior = if (settingsUiState.isStatusBarAutoHide) {
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        } else {
            WindowInsetsControllerCompat.BEHAVIOR_DEFAULT
        }
    }
    DisposableEffect(Unit) { onDispose { sensorController.unregister() } }
}