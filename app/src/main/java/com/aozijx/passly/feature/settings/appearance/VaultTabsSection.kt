package com.aozijx.passly.feature.settings.appearance

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Pin
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aozijx.passly.R
import com.aozijx.passly.core.ui.components.group.GroupCard
import com.aozijx.passly.core.ui.components.group.RoundedGroup
import com.aozijx.passly.core.ui.components.group.RoundedGroupItem
import com.aozijx.passly.core.ui.components.settings.SettingsSectionTitle
import com.aozijx.passly.feature.vault.model.VaultTab
import com.aozijx.passly.feature.vault.presentation.titleRes

@Composable
fun VaultTabsSettingsSection(
    enabledVaultTabKeys: Set<String>,
    onVaultTabToggle: (VaultTab) -> Unit
) {
    val toggleableTabs = VaultTab.toggleableVisibleTabs

    SettingsSectionTitle(text = stringResource(R.string.settings_interface_vault_tabs_section))
    RoundedGroup(
        items = listOf(
            RoundedGroupItem(key = "interface.vault_tabs") { itemScope ->
                GroupCard(itemScope = itemScope, contentPadding = PaddingValues(0.dp)) {
                    FlowRow(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        toggleableTabs.forEach { tab ->
                            val isChecked = tab.settingsKey in enabledVaultTabKeys
                            FilterChip(
                                selected = isChecked,
                                onClick = { onVaultTabToggle(tab) },
                                label = { Text(stringResource(tab.titleRes)) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = tab.settingsIcon(),
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

private fun VaultTab.settingsIcon(): ImageVector = when (this) {
    VaultTab.PASSWORDS -> Icons.Default.Key
    VaultTab.TOTP -> Icons.Default.Pin
    VaultTab.ALL -> Icons.Default.Key
}
