package com.aozijx.passly.ui.features.settings.appearance

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
import com.aozijx.passly.domain.config.AppDefaults
import com.aozijx.passly.ui.features.settings.components.GroupCard
import com.aozijx.passly.ui.features.settings.shell.SettingsGroupTitle
import com.aozijx.passly.ui.features.settings.shell.SettingsRoundedGroup
import com.aozijx.passly.ui.features.vault.model.VaultTab
import kotlin.math.roundToInt

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
        AppDefaults.TAB_THRESHOLD_MIN,
        AppDefaults.TAB_THRESHOLD_MAX
    )
    var sliderValue by remember(persistedThreshold) { mutableFloatStateOf(persistedThreshold.toFloat()) }
    val previewThreshold = sliderValue.roundToInt()
        .coerceIn(AppDefaults.TAB_THRESHOLD_MIN, AppDefaults.TAB_THRESHOLD_MAX)

    SettingsGroupTitle(text = "保险箱 Tab")
    SettingsRoundedGroup {
        item { position ->
            GroupCard(position = position, contentPadding = PaddingValues(0.dp)) {
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
                            AppDefaults.TAB_THRESHOLD_MIN.toFloat(),
                            AppDefaults.TAB_THRESHOLD_MAX.toFloat()
                        )
                    },
                    onValueChangeFinished = {
                        if (previewThreshold != persistedThreshold)
                            onTabBarMaxTabsWithoutScrollChange(previewThreshold)
                    },
                    valueRange = AppDefaults.TAB_THRESHOLD_MIN.toFloat()..AppDefaults.TAB_THRESHOLD_MAX.toFloat(),
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
    }
}