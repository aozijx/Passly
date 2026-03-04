package com.example.poop.ui.screens.vault

import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.FragmentActivity
import com.example.poop.ui.screens.vault.components.layout.VaultContent
import com.example.poop.ui.screens.vault.utils.BiometricHelper
import com.example.poop.ui.theme.PoopTheme

class VaultActivity : FragmentActivity() {
    private val viewModel: VaultViewModel by viewModels()

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

        // 初始验证
        requestAuthentication(isFirstTime = true)

        setContent {
            val isDarkModePref by viewModel.isDarkMode.collectAsState()
            val isDynamicColorPref by viewModel.isDynamicColor.collectAsState()

            PoopTheme(
                darkTheme = if (isDarkModePref == true) true else null,
                dynamicColor = isDynamicColorPref
            ) {
                // 响应 ViewModel 中的授权状态变化
                if (viewModel.isAuthorized) {
                    VaultContent(this, viewModel)
                } else {
                    AuthorizationPlaceholder { requestAuthentication(isFirstTime = false) }
                }
            }
        }
    }

    private fun requestAuthentication(isFirstTime: Boolean) {
        BiometricHelper.authenticate(
            activity = this,
            title = "安全验证",
            subtitle = "验证身份以访问保险箱",
            onSuccess = {
                viewModel.authorize()
            },
            onError = { error ->
                if (isFirstTime) {
                    Toast.makeText(this, "安全验证失败: $error", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    Toast.makeText(this, "验证未通过", Toast.LENGTH_SHORT).show()
                }
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
        if (viewModel.isAuthorized) {
            viewModel.startLockTimer()
        }
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
                "保险箱已锁定",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                "点击屏幕以解锁",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
