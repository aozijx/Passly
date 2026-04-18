package com.aozijx.passly.features.settings.components.sections

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aozijx.passly.features.settings.components.common.SettingsCard
import com.aozijx.passly.features.settings.components.common.SettingsGroupTitle
import com.aozijx.passly.features.settings.components.common.SwitchSettingItem
import com.aozijx.passly.features.vault.model.VaultTab

/**
 * 保险箱 Tab 可见性设置。
 *
 * 仅展示 [VaultTab.isToggleable] 为 true 的条目；
 * 不可切换的 Tab（如 ALL）始终可见，因此不出现在设置中。
 */
@Composable
fun VaultTabsSettingsSection(
    visibleVaultTabs: Set<String>?,
    onVisibleVaultTabsChange: (Set<String>) -> Unit
) {
    val enabledKeys = visibleVaultTabs ?: VaultTab.defaultVisibleKeys
    val toggleableTabs = VaultTab.entries.filter { it.isToggleable }

    SettingsGroupTitle(text = "保险箱 Tab")
    SettingsCard {
        toggleableTabs.forEachIndexed { index, tab ->
            if (index > 0) {
                HorizontalDivider(Modifier.padding(start = 56.dp, end = 16.dp), thickness = 0.5.dp)
            }
            val isChecked = tab.settingsKey in enabledKeys
            SwitchSettingItem(
                icon = tab.icon,
                title = stringResource(tab.titleRes),
                subtitle = null,
                checked = isChecked,
                onCheckedChange = { checked ->
                    val next = buildSet {
                        // 保留所有不可切换的 Tab
                        VaultTab.entries.filter { !it.isToggleable }.forEach { add(it.settingsKey) }
                        // 保留当前仍启用的其它可切换 Tab
                        toggleableTabs.filter { it != tab && it.settingsKey in enabledKeys }
                            .forEach { add(it.settingsKey) }
                        if (checked) add(tab.settingsKey)
                    }
                    onVisibleVaultTabsChange(next)
                }
            )
        }
        // 避免最后一项下方视觉上过于紧凑
        Column(Modifier.padding(bottom = 4.dp)) {}
    }
}