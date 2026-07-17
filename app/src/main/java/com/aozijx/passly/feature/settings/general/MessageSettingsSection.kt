package com.aozijx.passly.feature.settings.general

import androidx.compose.runtime.Composable
import com.aozijx.passly.feature.settings.components.switchSettingsItem
import com.aozijx.passly.feature.settings.shell.SettingsGroupTitle
import com.aozijx.passly.feature.settings.shell.SettingsRoundedGroup

@Composable
internal fun MessageSettingsSection(
    state: MessageSettingsUiState,
    onShowGeneralChange: (Boolean) -> Unit,
    onShowIconDownloadsChange: (Boolean) -> Unit,
    onShowClipboardClearsChange: (Boolean) -> Unit,
    onShowAppCloseChange: (Boolean) -> Unit
) {
    SettingsGroupTitle(text = "消息通知")
    SettingsRoundedGroup {
        switchSettingsItem(
            title = "应用内消息",
            subtitle = "控制应用内操作结果和错误提示",
            checked = state.showGeneral,
            onCheckedChange = onShowGeneralChange
        )
        switchSettingsItem(
            visible = state.showGeneral,
            title = "图标下载通知",
            subtitle = "显示网站图标下载结果",
            checked = state.showIconDownloads,
            onCheckedChange = onShowIconDownloadsChange
        )
        switchSettingsItem(
            visible = state.showGeneral,
            title = "剪贴板清除通知",
            subtitle = "敏感内容从剪贴板移除后提醒",
            checked = state.showClipboardClears,
            onCheckedChange = onShowClipboardClearsChange
        )
        switchSettingsItem(
            visible = state.showGeneral,
            title = "应用关闭通知",
            subtitle = "通过翻转手势关闭应用前提醒",
            checked = state.showAppClose,
            onCheckedChange = onShowAppCloseChange
        )
    }
}
