package com.aozijx.passly.feature.settings.security.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.Security
import androidx.compose.runtime.Composable
import com.aozijx.passly.ui.components.group.RoundedGroup
import com.aozijx.passly.ui.components.group.switchSettingsGroupItem
import com.aozijx.passly.ui.components.settings.SettingsSectionTitle

@Composable
fun SecurityProtectionSettingsSection(
    isSecureContentEnabled: Boolean,
    isFlipToLockEnabled: Boolean,
    isFlipExitAndClearStackEnabled: Boolean,
    onSecureContentEnabledChange: (Boolean) -> Unit,
    onFlipToLockEnabledChange: (Boolean) -> Unit,
    onFlipExitAndClearStackEnabledChange: (Boolean) -> Unit
) {
    SettingsSectionTitle(text = "安全防护")
    RoundedGroup(
        items = listOf(
            switchSettingsGroupItem(
                key = "privacy.secure_content",
                icon = Icons.Default.Security,
                title = "安全防护",
                subtitle = "禁止截屏录屏，并隐藏多任务预览",
                checked = isSecureContentEnabled,
                onCheckedChange = onSecureContentEnabledChange
            ),
            switchSettingsGroupItem(
                key = "privacy.flip_to_lock",
                icon = Icons.Default.Flip,
                title = "锁定应用",
                subtitle = "屏幕朝下时立即关闭保险箱",
                checked = isFlipToLockEnabled,
                onCheckedChange = onFlipToLockEnabledChange
            ),
            switchSettingsGroupItem(
                key = "privacy.flip_exit",
                visible = isFlipToLockEnabled,
                iconPlaceholder = true,
                title = "退出应用",
                subtitle = "退出应用并清除任务栈",
                checked = isFlipExitAndClearStackEnabled,
                onCheckedChange = onFlipExitAndClearStackEnabledChange
            )
        )
    )
}
