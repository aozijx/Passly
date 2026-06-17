package com.aozijx.passly.ui.features.settings.security

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Timer
import androidx.compose.runtime.Composable
import com.aozijx.passly.ui.features.settings.components.navigationSettingsItem
import com.aozijx.passly.ui.features.settings.components.switchSettingsItem
import com.aozijx.passly.ui.features.settings.shell.SettingsGroupTitle
import com.aozijx.passly.ui.features.settings.shell.SettingsRoundedGroup

@Composable
fun LockAuthSettingsSection(
    lockTimeout: Long,
    isAppPasswordEnabled: Boolean,
    isPasswordPreferredAuthFirst: Boolean,
    isDeviceCredentialFallbackEnabled: Boolean,
    isInvalidateKeyOnBioChange: Boolean,
    onLockTimeoutClick: () -> Unit,
    onAppPasswordClick: () -> Unit,
    onPasswordPreferredAuthFirstChange: (Boolean) -> Unit,
    onDeviceCredentialFallbackToggleRequested: (Boolean) -> Unit,
    onInvalidateKeyOnBioChangeToggle: (Boolean) -> Unit
) {
    SettingsGroupTitle(text = "锁定与认证")
    SettingsRoundedGroup {
        navigationSettingsItem(
            icon = Icons.Default.Timer,
            title = "自动锁定",
            value = formatLockTimeoutText(lockTimeout),
            onClick = onLockTimeoutClick
        )
        navigationSettingsItem(
            icon = Icons.Default.Lock,
            title = "设置密码",
            subtitle = "独立于系统锁屏，用密码直接解锁应用",
            value = if (isAppPasswordEnabled) "已设置" else "未设置",
            onClick = onAppPasswordClick
        )
        switchSettingsItem(
            icon = Icons.Default.Lock,
            title = "优先验证应用密码",
            subtitle = "解锁页优先显示应用密码入口",
            checked = isPasswordPreferredAuthFirst,
            onCheckedChange = onPasswordPreferredAuthFirstChange
        )
        switchSettingsItem(
            icon = Icons.Default.Fingerprint,
            title = "设备凭据作为兜底",
            subtitle = "使用系统PIN/图案/密码验证",
            checked = isDeviceCredentialFallbackEnabled,
            onCheckedChange = onDeviceCredentialFallbackToggleRequested
        )
        switchSettingsItem(
            icon = Icons.Default.Fingerprint,
            title = "生物识别变更时销毁密钥",
            subtitle = if (isInvalidateKeyOnBioChange)
                "新增或移除指纹/面部时，保险箱密钥将被销毁"
            else
                "新增或移除指纹/面部时，保险箱密钥保持有效",
            checked = isInvalidateKeyOnBioChange,
            onCheckedChange = onInvalidateKeyOnBioChangeToggle
        )
    }
}