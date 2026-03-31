package com.example.poop

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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.poop.features.settings.SettingsScreen
import com.example.poop.features.settings.SettingsViewModel
import com.example.poop.features.vault.VaultContent
import com.example.poop.features.vault.VaultViewModel
import com.example.poop.ui.theme.PoopTheme

class MainActivity : FragmentActivity() {
    private val viewModel: MainViewModel by viewModels()
    private var showSettings by mutableStateOf(false)
    private var showDetail by mutableStateOf(false)
    private var detailEntry by mutableStateOf<com.example.poop.data.model.VaultEntry?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 安全增强：禁止截屏和多任务预览
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE,
        )
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

            PoopTheme(
                darkTheme = if (isDarkModePref == true) true else null,
                dynamicColor = isDynamicColorPref
            ) {
                when {
                    showDetail && detailEntry != null -> {
                        com.example.poop.features.detail.DetailScreen(
                            entry = detailEntry!!,
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

    private fun requestAuthentication() {
        viewModel.authenticate(
            activity = this,
            title = getString(R.string.vault_auth_decrypt_title),
            subtitle = getString(R.string.vault_auth_subtitle),
            onSuccess = {
                viewModel.authorize()
            },
            onError = { _ ->
                // 即使第一次认证失败也不退出应用，让用户可以重试
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
