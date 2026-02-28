package com.example.poop.ui.screens.settings

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.poop.ui.navigation.TopBarConfig
import com.example.poop.ui.screens.vault.VaultActivity
import com.example.poop.util.BiometricHelper
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    
    // 点击计次
    var versionTapCount by remember { mutableIntStateOf(0) }
    // 失败计次
    var authFailureCount by remember { mutableIntStateOf(0) }
    val maxFailures = 3

    LaunchedEffect(versionTapCount) {
        if (versionTapCount > 0) {
            delay(2000)
            versionTapCount = 0
        }
    }

    TopBarConfig(
        title = "设置",
        centerTitle = true
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize()
    ) {
        item { SectionTitle("通用") }
        item {
            SettingsSwitchItem(
                title = "开启消息通知",
                checked = uiState.isNotificationsEnabled,
                onCheckedChange = viewModel::toggleNotifications
            )
        }
        item {
            SettingsSwitchItem(
                title = "深色模式",
                subtitle = if (uiState.isDarkMode) "已开启" else "跟随系统",
                checked = uiState.isDarkMode,
                onCheckedChange = viewModel::toggleDarkMode
            )
        }

        item { SectionTitle("关于") }
        item {
            SettingsClickableItem(
                title = "清除缓存",
                value = uiState.cacheSize,
                onClick = { viewModel.clearCache() }
            )
        }
        item {
            SettingsClickableItem(
                title = "关于我们",
                onClick = { /* TODO: 跳转到关于页 */ }
            )
        }
        item {
            SettingsClickableItem(
                title = "版本号",
                value = "v1.0.0",
                onClick = {
                    // 如果失败次数过多，直接拦截，不再弹出验证器
                    if (authFailureCount >= maxFailures) {
                        Toast.makeText(context, "为了安全，尝试次数过多，请稍后再试", Toast.LENGTH_LONG).show()
                        return@SettingsClickableItem
                    }

                    versionTapCount++
                    if (versionTapCount >= 3) {
                        val activity = context as? FragmentActivity
                        if (activity != null) {
                            BiometricHelper.showBiometricPrompt(
                                activity = activity,
                                onSuccess = {
                                    authFailureCount = 0 // 验证成功，重置失败计数
                                    context.startActivity(Intent(context, VaultActivity::class.java))
                                    versionTapCount = 0
                                },
                                onError = { _ ->
                                    authFailureCount++ // 增加失败计数
                                    val remaining = maxFailures - authFailureCount
                                    if (remaining > 0) {
                                        Toast.makeText(context, "验证失败，还剩 $remaining 次机会", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "验证已锁定，请稍后重新进入", Toast.LENGTH_LONG).show()
                                    }
                                }
                            )
                        } else {
                            context.startActivity(Intent(context, VaultActivity::class.java))
                            versionTapCount = 0
                        }
                    }
                }
            )
        }

        item {
            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = { /* TODO: 执行退出登录 */ },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("退出登录")
            }
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
    )
}

@Composable
fun SettingsSwitchItem(
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = null
        )
    }
}

@Composable
fun SettingsClickableItem(
    title: String,
    value: String? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        if (value != null) {
            Text(
                value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            "详情",
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
