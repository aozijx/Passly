package com.aozijx.passly

import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.aozijx.passly.core.designsystem.components.PlainExportDialog
import com.aozijx.passly.core.designsystem.components.PlainExportDialogType
import com.aozijx.passly.core.navigation.PasslyNavHost
import com.aozijx.passly.core.theme.AppTheme
import com.aozijx.passly.data.local.config.DatabaseConfig
import com.aozijx.passly.features.auth.ui.AuthScreen
import com.aozijx.passly.features.main.MainNotificationPermissionController
import com.aozijx.passly.features.main.MainSensorController
import com.aozijx.passly.features.main.MainViewModel
import com.aozijx.passly.features.main.contract.MainEffect
import com.aozijx.passly.features.main.contract.MainIntent
import com.aozijx.passly.features.settings.SettingsViewModel
import com.aozijx.passly.features.vault.VaultViewModel
import kotlin.system.exitProcess

class MainActivity : FragmentActivity() {
    private val viewModel: MainViewModel by viewModels()

    private val sensorController: MainSensorController by lazy {
        MainSensorController(this) {
            if (viewModel.auth.isAuthorized.value) {
                viewModel.handleIntent(MainIntent.Lock)
                if (sensorController.isFlipExitAndClearStackEnabled) finishAndRemoveTask()
            }
        }
    }

    private val notificationPermissionController: MainNotificationPermissionController by lazy {
        MainNotificationPermissionController(this) {
            Toast.makeText(
                this,
                getString(R.string.main_notification_permission_denied),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sensorController.initialize()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        enableEdgeToEdge()

        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        notificationPermissionController.requestIfNeeded()

        setContent {
            val mainUiState by viewModel.uiState.collectAsStateWithLifecycle()
            val context = LocalContext.current
            val settingsViewModel: SettingsViewModel = viewModel()

            // 全局备份消息监听
            LaunchedEffect(settingsViewModel.backup.backupMessage) {
                settingsViewModel.backup.backupMessage?.let {
                    Toast.makeText(context, it, Toast.LENGTH_LONG).show()
                    settingsViewModel.backup.clearBackupMessage()
                }
            }

            val plainExportPickerLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.CreateDocument("application/json")
            ) { uri ->
                uri?.let { viewModel.handleIntent(MainIntent.ExportPlainBackupToUri(context, it)) }
            }

            // 监听 MainEffect
            LaunchedEffect(Unit) {
                viewModel.effects.collect { effect ->
                    when (effect) {
                        is MainEffect.ShowToast -> Toast.makeText(
                            this@MainActivity,
                            effect.message,
                            Toast.LENGTH_SHORT
                        ).show()

                        is MainEffect.ShowError -> Toast.makeText(
                            this@MainActivity,
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
                // 根据状态机决定顶级视图
                when {
                    mainUiState.databaseError != null -> {
                        PlainExportDialog(
                            type = PlainExportDialogType.DatabaseError,
                            onExportBackup = {
                                viewModel.handleIntent(
                                    MainIntent.ExportEmergencyBackup(
                                        context
                                    )
                                )
                            },
                            onResetOrCancel = {
                                context.deleteDatabase(DatabaseConfig.DATABASE_NAME)
                                Toast.makeText(
                                    context,
                                    "数据库已清除，请重启应用",
                                    Toast.LENGTH_SHORT
                                ).show()
                                finishAffinity()
                                exitProcess(0)
                            }
                        )
                    }

                    mainUiState.isAuthorized -> {
                        AppMainContent(viewModel, settingsViewModel)
                    }

                    else -> {
                        AuthScreen(authCoordinator = viewModel.auth, activity = this)
                    }
                }
            }

            // 安全策略与系统 UI 设置
            val settingsUiState by settingsViewModel.uiState.collectAsStateWithLifecycle()
            LaunchedEffect(settingsUiState.isSecureContentEnabled) {
                if (settingsUiState.isSecureContentEnabled) {
                    window.setFlags(
                        WindowManager.LayoutParams.FLAG_SECURE,
                        WindowManager.LayoutParams.FLAG_SECURE
                    )
                } else {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                }
            }
            LaunchedEffect(settingsUiState.isFlipToLockEnabled) {
                sensorController.isFlipLockEnabled = settingsUiState.isFlipToLockEnabled
                if (settingsUiState.isFlipToLockEnabled) sensorController.register() else sensorController.unregister()
            }
            LaunchedEffect(settingsUiState.isStatusBarAutoHide) {
                val insetsController = WindowCompat.getInsetsController(window, window.decorView)
                insetsController.systemBarsBehavior = if (settingsUiState.isStatusBarAutoHide) {
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                } else {
                    WindowInsetsControllerCompat.BEHAVIOR_DEFAULT
                }
            }
            DisposableEffect(Unit) { onDispose { sensorController.unregister() } }
        }
    }

    @Composable
    private fun AppMainContent(mainViewModel: MainViewModel, settingsViewModel: SettingsViewModel) {
        val context = LocalContext.current
        val vaultViewModel: VaultViewModel = viewModel()
        val settingsUiState by settingsViewModel.uiState.collectAsStateWithLifecycle()
        var showPlainExportRiskDialog by remember { mutableStateOf(false) }
        val navController = rememberNavController()

        PasslyNavHost(
            navController = navController,
            activity = this,
            mainViewModel = mainViewModel,
            vaultViewModel = vaultViewModel,
            settingsViewModel = settingsViewModel,
            onPlainExportClick = { showPlainExportRiskDialog = true }
        )

        if (showPlainExportRiskDialog) {
            PlainExportDialog(
                type = PlainExportDialogType.NormalExport,
                onExportBackup = {
                    showPlainExportRiskDialog = false
                    mainViewModel.auth.authenticate(
                        activity = this,
                        title = getString(R.string.vault_backup_auth_title),
                        subtitle = getString(R.string.vault_backup_auth_subtitle_plain_export),
                        onSuccess = {
                            mainViewModel.handleIntent(
                                MainIntent.ExportPlainBackup(
                                    context,
                                    settingsUiState.backupDirectoryUri
                                )
                            )
                        }
                    )
                },
                onResetOrCancel = { showPlainExportRiskDialog = false }
            )
        }
    }

    override fun onUserInteraction() {
        super.onUserInteraction()
        viewModel.handleIntent(MainIntent.UpdateInteraction)
    }

    override fun onResume() {
        super.onResume()
        viewModel.handleIntent(MainIntent.CheckAndLock)
        if (sensorController.isFlipLockEnabled) sensorController.register()
    }

    override fun onPause() {
        super.onPause()
        if (sensorController.isFlipLockEnabled) sensorController.unregister()
    }
}