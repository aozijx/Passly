package com.aozijx.passly.ui.features.main.ui

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
import com.aozijx.passly.core.di.appViewModelFactory
import com.aozijx.passly.data.local.DatabaseConfig
import com.aozijx.passly.domain.config.UserConfigProvider
import com.aozijx.passly.ui.features.backup.components.PlainExportDialog
import com.aozijx.passly.ui.features.backup.components.PlainExportDialogType
import com.aozijx.passly.ui.features.main.MainSensorController
import com.aozijx.passly.ui.features.main.MainViewModel
import com.aozijx.passly.ui.features.main.contract.MainEffect
import com.aozijx.passly.ui.features.main.contract.MainIntent
import com.aozijx.passly.ui.features.verification.VerificationScreen
import com.aozijx.passly.ui.features.verification.VerificationViewModel
import com.aozijx.passly.ui.theme.AppTheme
import kotlin.system.exitProcess

@Composable
internal fun MainScreen(
    activity: FragmentActivity,
    viewModel: MainViewModel,
    sensorController: MainSensorController
) {
    val mainUiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val factory = appViewModelFactory(activity.application)

    val configProvider: UserConfigProvider = viewModel(factory = factory)
    val userConfig by configProvider.config.collectAsStateWithLifecycle()

    val verificationViewModel: VerificationViewModel = viewModel(factory = factory)

    LaunchedEffect(configProvider.backup.backupMessage) {
        configProvider.backup.backupMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            configProvider.backup.clearBackupMessage()
        }
    }

    val plainExportPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let { configProvider.backup.exportPlainBackupToUri(it) }
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

            mainUiState.isAuthorized -> {
                AppMainContent(
                    activity = activity,
                    mainViewModel = viewModel,
                    onPlainExportPickerRequest = { fileName ->
                        plainExportPickerLauncher.launch(fileName)
                    }
                )
            }

            else -> {
                VerificationScreen(
                    viewModel = verificationViewModel,
                    activity = activity,
                    preferPasswordFirst = userConfig.security.isPasswordPreferredAuthFirst
                )
            }
        }
    }

    val window = activity.window

    LaunchedEffect(
        userConfig.security.isSecureContentEnabled,
        userConfig.security.isFlipToLockEnabled,
        userConfig.security.isFlipExitAndClearStackEnabled,
        userConfig.display.isStatusBarAutoHide
    ) {
        if (userConfig.security.isSecureContentEnabled) {
            window.setFlags(
                WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE
            )
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }

        sensorController.isFlipLockEnabled = userConfig.security.isFlipToLockEnabled
        if (userConfig.security.isFlipToLockEnabled) sensorController.register() else sensorController.unregister()

        sensorController.isFlipExitAndClearStackEnabled =
            userConfig.security.isFlipExitAndClearStackEnabled

        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        insetsController.systemBarsBehavior = if (userConfig.display.isStatusBarAutoHide) {
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        } else {
            WindowInsetsControllerCompat.BEHAVIOR_DEFAULT
        }
    }
    DisposableEffect(Unit) { onDispose { sensorController.unregister() } }
}