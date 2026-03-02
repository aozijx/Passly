package com.example.poop.ui.screens.vault

import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.FragmentActivity
import com.example.poop.data.Preference
import com.example.poop.ui.theme.PoopTheme
import com.example.poop.util.BiometricHelper

class VaultActivity : FragmentActivity() {
    private val viewModel: VaultViewModel by viewModels()
    private var isAuthorized by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE,
        )
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        
        // 开启边到边显示（使内容可以延伸到状态栏下方）
        enableEdgeToEdge()
        
        // 配置系统栏行为：粘性沉浸模式（滑动显示后自动恢复隐藏）
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.systemBarsBehavior = 
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        // 验证身份
        BiometricHelper.authenticate(
            activity = this,
            title = "安全验证",
            subtitle = "验证身份以访问保险箱",
            onSuccess = { isAuthorized = true },
            onError = { error ->
                Toast.makeText(this, "安全验证失败: $error", Toast.LENGTH_SHORT).show()
                finish()
            }
        )

        setContent {
            val preference = remember { Preference(applicationContext) }
            val isDarkModePref by preference.isDarkMode.collectAsState(initial = null)
            val isDynamicColorPref by preference.isDynamicColor.collectAsState(initial = true)

            PoopTheme(
                darkTheme = if (isDarkModePref == true) true else null,
                dynamicColor = isDynamicColorPref
            ) {
                if (isAuthorized) {
                    VaultContent(this, viewModel)
                } else {
                    AuthorizationPlaceholder()
                }
            }
        }
    }
}

@androidx.compose.runtime.Composable
private fun AuthorizationPlaceholder() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
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
            Text("正在进行安全验证...", color = MaterialTheme.colorScheme.onSurface)
        }
    }
}
