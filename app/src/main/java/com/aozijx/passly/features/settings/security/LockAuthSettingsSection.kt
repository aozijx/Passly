package com.aozijx.passly.features.settings.security

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aozijx.passly.features.settings.shell.ClickableSettingItem
import com.aozijx.passly.features.settings.shell.SettingsCard
import com.aozijx.passly.features.settings.shell.SettingsGroupTitle
import com.aozijx.passly.features.settings.shell.SwitchSettingItem
import com.aozijx.passly.features.settings.shell.formatLockTimeoutText

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
    SettingsCard {
        ClickableSettingItem(
            icon = Icons.Default.Timer,
            title = "自动锁定时间",
            value = formatLockTimeoutText(lockTimeout),
            onClick = onLockTimeoutClick
        )
        HorizontalDivider(Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp)
        ClickableSettingItem(
            icon = Icons.Default.Lock,
            title = "设置密码",
            value = if (isAppPasswordEnabled) "已设置" else "未设置",
            longValue = "独立于系统锁屏，可用密码直接解锁应用",
            onClick = onAppPasswordClick
        )
        HorizontalDivider(Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp)
        SwitchSettingItem(
            icon = Icons.Default.Lock,
            title = "优先验证应用密码",
            subtitle = "开启后，解锁页优先显示应用密码入口",
            checked = isPasswordPreferredAuthFirst,
            onCheckedChange = onPasswordPreferredAuthFirstChange
        )
        HorizontalDivider(Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp)
        SwitchSettingItem(
            icon = Icons.Default.Fingerprint,
            title = "允许设备凭据作为兜底",
            subtitle = "关闭后仅允许生物识别，不可使用系统PIN/图案/密码",
            checked = isDeviceCredentialFallbackEnabled,
            onCheckedChange = onDeviceCredentialFallbackToggleRequested
        )
        HorizontalDivider(Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp)
        SwitchSettingItem(
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