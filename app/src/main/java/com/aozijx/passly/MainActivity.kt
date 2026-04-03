package com.aozijx.passly

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aozijx.passly.domain.model.VaultEntry
import com.aozijx.passly.features.detail.page.DetailScreen
import com.aozijx.passly.features.settings.SettingsScreen
import com.aozijx.passly.features.settings.SettingsViewModel
import com.aozijx.passly.features.vault.VaultContent
import com.aozijx.passly.features.vault.VaultViewModel
import com.aozijx.passly.ui.theme.AppTheme

class MainActivity : FragmentActivity(), SensorEventListener {
    private val viewModel: MainViewModel by viewModels()
    private var showSettings by mutableStateOf(false)
    private var showDetail by mutableStateOf(false)
    private var detailEntry by mutableStateOf<VaultEntry?>(null)

    private var sensorManager: SensorManager? = null
    private var accelerometer: Sensor? = null
    private var isFlipLockEnabled = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

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
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })

        // 初始验证
        requestAuthentication()

        setContent {
            val isDarkModePref by viewModel.isDarkMode.collectAsState()
            val isDynamicColorPref by viewModel.isDynamicColor.collectAsState()
            val vaultViewModel: VaultViewModel = viewModel()
            val settingsViewModel: SettingsViewModel = viewModel()
            
            // 应用高级安全防护设置
            val isSecureContentEnabled by settingsViewModel.isSecureContentEnabled.collectAsStateWithLifecycle()
            
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
            val flipToLock by settingsViewModel.isFlipToLockEnabled.collectAsStateWithLifecycle()
            LaunchedEffect(flipToLock) {
                isFlipLockEnabled = flipToLock
                if (flipToLock) {
                    registerSensor()
                } else {
                    unregisterSensor()
                }
            }

            // 自动注销传感器
            DisposableEffect(Unit) {
                onDispose { unregisterSensor() }
            }
            
            // 应用状态栏自动隐藏行为设置
            val isStatusBarAutoHide by settingsViewModel.isStatusBarAutoHide.collectAsStateWithLifecycle()
            LaunchedEffect(isStatusBarAutoHide) {
                val insetsController = WindowCompat.getInsetsController(window, window.decorView)
                insetsController.systemBarsBehavior = if (isStatusBarAutoHide) {
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                } else {
                    WindowInsetsControllerCompat.BEHAVIOR_DEFAULT
                }
            }

            AppTheme(
                darkTheme = if (isDarkModePref == true) true else null,
                dynamicColor = isDynamicColorPref
            ) {
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
                    viewModel.isAuthorized -> {
                        VaultContent(
                            activity = this,
                            mainViewModel = viewModel,
                            vaultViewModel = vaultViewModel,
                            settingsViewModel = settingsViewModel,
                            onSettingsClick = { showSettings = true },
                            onShowDetail = { entry -> 
                                detailEntry = entry
                                showDetail = true
                            }
                        )
                    }
                    else -> {
                        AuthorizationPlaceholder { requestAuthentication() }
                    }
                }
            }
        }
    }

    private fun registerSensor() {
        accelerometer?.let {
            sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    private fun unregisterSensor() {
        sensorManager?.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (!isFlipLockEnabled || event?.sensor?.type != Sensor.TYPE_ACCELEROMETER) return
        
        val z = event.values[2]
        // Z 轴负值表示屏幕朝下，通常 -9.8 左右是完全平放。这里取 -8.0 作为触发阈值。
        if (z < -8.5f && viewModel.isAuthorized) {
            viewModel.lock()
            showDetail = false
            showSettings = false
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private fun requestAuthentication() {
        viewModel.authenticate(
            activity = this,
            title = getString(R.string.vault_auth_decrypt_title),
            subtitle = getString(R.string.vault_auth_subtitle),
            onSuccess = {
                viewModel.authorize()
            },
            onError = { _ ->
                Toast.makeText(this, getString(R.string.vault_auth_failed), Toast.LENGTH_SHORT).show()
            }
        )
    }

    override fun onUserInteraction() {
        super.onUserInteraction()
        viewModel.updateInteraction()
    }

    override fun onResume() {
        super.onResume()
        viewModel.checkAndLock()
        if (isFlipLockEnabled) registerSensor()
    }

    override fun onPause() {
        super.onPause()
        if (isFlipLockEnabled) unregisterSensor()
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
            ),
        contentAlignment = Alignment.Center
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



