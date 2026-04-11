package com.aozijx.passly

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aozijx.passly.core.designsystem.components.PlainExportDialog
import com.aozijx.passly.core.designsystem.components.PlainExportDialogType
import com.aozijx.passly.core.theme.AppTheme
import com.aozijx.passly.data.local.config.DatabaseConfig
import com.aozijx.passly.domain.model.core.VaultEntry
import com.aozijx.passly.features.detail.page.DetailScreen
import com.aozijx.passly.features.main.MainSensorController
import com.aozijx.passly.features.main.MainViewModel
import com.aozijx.passly.features.main.contract.MainEffect
import com.aozijx.passly.features.main.contract.MainIntent
import com.aozijx.passly.features.settings.SettingsScreen
import com.aozijx.passly.features.settings.SettingsViewModel
import com.aozijx.passly.features.vault.VaultContent
import com.aozijx.passly.features.vault.VaultViewModel
import kotlin.system.exitProcess

class MainActivity : FragmentActivity() {
    private val viewModel: MainViewModel by viewModels()
    private var showSettings by mutableStateOf(false)
    private var showDetail by mutableStateOf(false)
    private var detailEntry by mutableStateOf<VaultEntry?>(null)

    private val sensorController: MainSensorController by lazy {
        MainSensorController(this) {
            if (viewModel.isAuthorized) {
                viewModel.handleIntent(MainIntent.Lock)
                showDetail = false
                showSettings = false
                if (sensorController.isFlipExitAndClearStackEnabled) finishAndRemoveTask()
            }
        }
    }

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (!granted) {
                Toast.makeText(this, "通知权限未开启，后台同步进度将不可见", Toast.LENGTH_SHORT)
                    .show()
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

        // 添加返回按钮回调
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (showDetail) {
                    showDetail = false
                } else if (showSettings) {
                    showSettings = false
                } else {
                    finish()
                }
            }
        })

        requestNotificationPermissionIfNeeded()

        setContent {
            val mainUiState by viewModel.uiState.collectAsStateWithLifecycle()
            val context = LocalContext.current

            // 状态驱动认证：DB 就绪且未授权时触发
            LaunchedEffect(
                mainUiState.isDatabaseInitializing,
                mainUiState.databaseError,
                mainUiState.isAuthorized
            ) {
                if (!mainUiState.isDatabaseInitializing
                    && mainUiState.databaseError == null
                    && !mainUiState.isAuthorized
                ) {
                    requestAuthentication()
                }
            }

            // 收集 MainViewModel 单次事件
            LaunchedEffect(Unit) {
                viewModel.effects.collect { effect ->
                    when (effect) {
                        is MainEffect.ShowToast ->
                            Toast.makeText(this@MainActivity, effect.message, Toast.LENGTH_SHORT).show()
                        is MainEffect.ShowError ->
                            Toast.makeText(this@MainActivity, effect.error, Toast.LENGTH_LONG).show()
                        MainEffect.LockedByTimeout -> {
                            showDetail = false
                            showSettings = false
                        }
                        MainEffect.NavigateToVault -> { /* uiState.isAuthorized 已驱动 UI 分支 */ }
                    }
                }
            }

            // 数据库错误弹窗
            if (mainUiState.databaseError != null) {
                PlainExportDialog(
                    type = PlainExportDialogType.DatabaseError,
                    onExportBackup = { viewModel.handleIntent(MainIntent.ExportEmergencyBackup(context)) },
                    onResetOrCancel = {
                        context.deleteDatabase(DatabaseConfig.DATABASE_NAME)
                        Toast.makeText(context, "数据库已清除，请重启应用", Toast.LENGTH_SHORT)
                            .show()
                        finishAffinity()
                        exitProcess(0)
                    })
            }

            AppTheme(
                darkTheme = if (mainUiState.isDarkMode == true) true else null,
                dynamicColor = mainUiState.isDynamicColor
            ) {
                // 只有在没有数据库错误时才渲染业务界面
                if (mainUiState.databaseError == null) {
                    val vaultViewModel: VaultViewModel = viewModel()
                    val settingsViewModel: SettingsViewModel = viewModel()
                    val settingsUiState by settingsViewModel.uiState.collectAsStateWithLifecycle()

                    // 应用高级安全防护设置
                    val isSecureContentEnabled = settingsUiState.isSecureContentEnabled

                    LaunchedEffect(isSecureContentEnabled) {
                        if (isSecureContentEnabled) {
                            window.setFlags(
                                WindowManager.LayoutParams.FLAG_SECURE,
                                WindowManager.LayoutParams.FLAG_SECURE
                            )
                        } else {
                            window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                        }
                    }

                    // 翻转锁定逻辑
                    val flipToLock = settingsUiState.isFlipToLockEnabled
                    val flipExitAndClearStack = settingsUiState.isFlipExitAndClearStackEnabled
                    LaunchedEffect(flipToLock) {
                        sensorController.isFlipLockEnabled = flipToLock
                        if (flipToLock) sensorController.register() else sensorController.unregister()
                    }
                    LaunchedEffect(flipExitAndClearStack) {
                        sensorController.isFlipExitAndClearStackEnabled = flipExitAndClearStack
                    }

                    // 自动注销传感器
                    DisposableEffect(Unit) {
                        onDispose { sensorController.unregister() }
                    }

                    // 应用状态栏自动隐藏行为设置
                    val isStatusBarAutoHide = settingsUiState.isStatusBarAutoHide
                    LaunchedEffect(isStatusBarAutoHide) {
                        val insetsController =
                            WindowCompat.getInsetsController(window, window.decorView)
                        insetsController.systemBarsBehavior = if (isStatusBarAutoHide) {
                            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                        } else {
                            WindowInsetsControllerCompat.BEHAVIOR_DEFAULT
                        }
                    }

                    when {
                        showDetail && detailEntry != null -> {
                            DetailScreen(
                                initialEntry = detailEntry!!,
                                onBack = { showDetail = false },
                                activity = this,
                                mainViewModel = viewModel,
                                vaultViewModel = vaultViewModel
                            )
                        }

                        showSettings -> {
                            SettingsScreen(onBack = { showSettings = false })
                        }

                        mainUiState.isAuthorized -> {
                            VaultContent(
                                activity = this,
                                mainViewModel = viewModel,
                                vaultViewModel = vaultViewModel,
                                settingsViewModel = settingsViewModel,
                                onSettingsClick = { showSettings = true },
                                onPlainExportClick = {
                                    viewModel.authenticate(
                                        activity = this,
                                        title = getString(R.string.vault_backup_auth_title),
                                        subtitle = getString(R.string.vault_backup_auth_subtitle_plain_export)
                                    ) {
                                        viewModel.handleIntent(MainIntent.ExportPlainBackup(this))
                                    }
                                },
                                onShowDetail = { entry ->
                                    detailEntry = entry
                                    showDetail = true
                                })
                        }

                        else -> {
                            AuthorizationPlaceholder { requestAuthentication() }
                        }
                    }
                } else {
                    // 数据库错误时的背景占位
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surface)
                    )
                }
            }
        }
    }

    private fun requestAuthentication() {
        viewModel.authenticate(
            activity = this,
            title = getString(R.string.vault_auth_decrypt_title),
            subtitle = getString(R.string.vault_auth_subtitle),
            onSuccess = {},
            onError = { _ ->
                Toast.makeText(this, getString(R.string.vault_auth_failed), Toast.LENGTH_SHORT)
                    .show()
            })
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED) return
        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
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

@Composable
private fun AuthorizationPlaceholder(onRetry: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onRetry
            ), contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Default.Lock,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                stringResource(R.string.vault_locked_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                stringResource(R.string.vault_locked_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}