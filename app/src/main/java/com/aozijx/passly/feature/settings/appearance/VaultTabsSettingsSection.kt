package com.aozijx.passly.feature.settings.appearance

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aozijx.passly.core.ui.components.group.GroupCard
import com.aozijx.passly.core.ui.components.group.RoundedGroup
import com.aozijx.passly.core.ui.components.group.RoundedGroupItem
import com.aozijx.passly.core.ui.components.settings.SettingsSectionTitle
import com.aozijx.passly.domain.settings.model.TabLayoutConstraints
import com.aozijx.passly.feature.vault.model.VaultTab
import kotlin.math.roundToInt

private const val TAB_THRESHOLD_MIN = TabLayoutConstraints.MIN_TABS_WITHOUT_SCROLL
private const val TAB_THRESHOLD_MAX = TabLayoutConstraints.MAX_TABS_WITHOUT_SCROLL

@Composable
fun VaultTabsSettingsSection(
    visibleVaultTabs: Set<String>?,
    tabBarMaxTabsWithoutScroll: Int,
    onTabBarMaxTabsWithoutScrollChange: (Int) -> Unit,
    onVisibleVaultTabsChange: (Set<String>) -> Unit
) {
    val enabledKeys = visibleVaultTabs ?: VaultTab.defaultVisibleKeys
    val toggleableTabs = VaultTab.toggleableVisibleTabs
    val persistedThreshold = tabBarMaxTabsWithoutScroll.coerceIn(
        TAB_THRESHOLD_MIN,
        TAB_THRESHOLD_MAX
    )
    var sliderValue by remember(persistedThreshold) { mutableFloatStateOf(persistedThreshold.toFloat()) }
    val previewThreshold = sliderValue.roundToInt()
        .coerceIn(TAB_THRESHOLD_MIN, TAB_THRESHOLD_MAX)

    SettingsSectionTitle(text = "保险箱 Tab")
    RoundedGroup(
        items = listOf(
            RoundedGroupItem(key = "interface.vault_tabs") { itemScope ->
                GroupCard(itemScope = itemScope, contentPadding = PaddingValues(0.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, top = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Tab 均分阈值")
                    Text(text = "$previewThreshold")
                }

                Slider(
                    value = sliderValue,
                    onValueChange = { value ->
                        sliderValue = value.coerceIn(
                            TAB_THRESHOLD_MIN.toFloat(),
                            TAB_THRESHOLD_MAX.toFloat()
                        )
                    },
                    onValueChangeFinished = {
                        if (previewThreshold != persistedThreshold)
                            onTabBarMaxTabsWithoutScrollChange(previewThreshold)
                    },
                    valueRange = TAB_THRESHOLD_MIN.toFloat()..TAB_THRESHOLD_MAX.toFloat(),
                    steps = 5,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )

                Text(
                    text = "当 Tab 数量小于等于阈值时均分宽度，超过则横向滚动。",
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))

                FlowRow(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    toggleableTabs.forEach { tab ->
                        val isChecked = tab.settingsKey in enabledKeys
                        FilterChip(
                            selected = isChecked,
                            onClick = {
                                val next = buildSet {
                                    VaultTab.entries.filter { !it.isToggleable }
                                        .forEach { add(it.settingsKey) }
                                    toggleableTabs.filter { it != tab && it.settingsKey in enabledKeys }
                                        .forEach { add(it.settingsKey) }
                                    if (!isChecked) add(tab.settingsKey)
                                }
                                onVisibleVaultTabsChange(next)
                            },
                            label = { Text(stringResource(tab.titleRes)) },
                            leadingIcon = {
                                Icon(
                                    imageVector = tab.icon,
                                    contentDescription = null
                                )
                            }
                        )
                    }
                }
            }
        }
        )
    )
}
