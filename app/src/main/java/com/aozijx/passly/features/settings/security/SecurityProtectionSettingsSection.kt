package com.aozijx.passly.features.settings.security

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aozijx.passly.features.settings.shell.SettingsCard
import com.aozijx.passly.features.settings.shell.SettingsGroupTitle
import com.aozijx.passly.features.settings.shell.SwitchSettingItem

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
    SettingsCard {
        SwitchSettingItem(
            icon = Icons.Default.Security,
            title = "高级安全防护",
            subtitle = "禁止截屏录屏，并隐藏多任务预览内容",
            checked = isSecureContentEnabled,
            onCheckedChange = onSecureContentEnabledChange
        )
        HorizontalDivider(Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp)
        SwitchSettingItem(
            icon = Icons.Default.Flip,
            title = "翻转即锁定",
            subtitle = "手机屏幕朝下放置时立即关闭保险箱",
            checked = isFlipToLockEnabled,
            onCheckedChange = onFlipToLockEnabledChange
        )

        AnimatedVisibility(
            visible = isFlipToLockEnabled,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Column {
                HorizontalDivider(
                    Modifier.padding(start = 56.dp, end = 16.dp), thickness = 0.5.dp
                )
                SwitchSettingItem(
                    title = "翻转后退出应用并清空任务栈",
                    subtitle = "开启后将直接退出到桌面，下次进入需重新认证",
                    checked = isFlipExitAndClearStackEnabled,
                    onCheckedChange = onFlipExitAndClearStackEnabledChange
                )
            }
        }
    }
}