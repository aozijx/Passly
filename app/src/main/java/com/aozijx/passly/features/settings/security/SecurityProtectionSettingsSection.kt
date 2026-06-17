package com.aozijx.passly.features.settings.security

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.Security
import androidx.compose.runtime.Composable
import com.aozijx.passly.features.settings.components.switchSettingsItem
import com.aozijx.passly.features.settings.shell.SettingsGroupTitle
import com.aozijx.passly.features.settings.shell.SettingsRoundedGroup

@Composable
fun SecurityProtectionSettingsSection(
    isSecureContentEnabled: Boolean,
    isFlipToLockEnabled: Boolean,
    isFlipExitAndClearStackEnabled: Boolean,
    onSecureContentEnabledChange: (Boolean) -> Unit,
    onFlipToLockEnabledChange: (Boolean) -> Unit,
    onFlipExitAndClearStackEnabledChange: (Boolean) -> Unit
) {
    SettingsGroupTitle(text = "安全防护")
    SettingsRoundedGroup {
        switchSettingsItem(
            icon = Icons.Default.Security,
            title = "高级安全防护",
            subtitle = "禁止截屏录屏，并隐藏多任务预览",
            checked = isSecureContentEnabled,
            onCheckedChange = onSecureContentEnabledChange
        )
        switchSettingsItem(
            icon = Icons.Default.Flip,
            title = "翻转锁定",
            subtitle = "屏幕朝下放置时立即关闭保险箱",
            checked = isFlipToLockEnabled,
            onCheckedChange = onFlipToLockEnabledChange
        )
        switchSettingsItem(
            visible = isFlipToLockEnabled,
            iconPlaceholder = true,
            title = "退出并清空任务栈",
            subtitle = "退出到桌面，再次进入需重新认证",
            checked = isFlipExitAndClearStackEnabled,
            onCheckedChange = onFlipExitAndClearStackEnabledChange
        )
    }
}