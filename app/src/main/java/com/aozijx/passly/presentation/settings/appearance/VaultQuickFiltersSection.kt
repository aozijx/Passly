package com.aozijx.passly.presentation.settings.appearance

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
import com.aozijx.passly.core.ui.components.group.model.RoundedGroupItem
import com.aozijx.passly.core.ui.components.settings.SettingsSectionTitle
import com.aozijx.passly.core.ui.components.vaultfilter.titleRes
import com.aozijx.passly.domain.settings.model.LibraryQuickFilter

@Composable
fun LibraryQuickFiltersSettingsSection(
    enabledLibraryQuickFilterKeys: Set<String>,
    onLibraryQuickFilterToggle: (LibraryQuickFilter) -> Unit
) {
    val toggleableQuickFilters = LibraryQuickFilter.toggleableVisibleQuickFilters

    SettingsSectionTitle(
        text = stringResource(R.string.settings_interface_vault_quick_filters_section)
    )
    RoundedGroup(
        items = listOf(
            RoundedGroupItem(key = "interface.vault_quick_filters") { itemScope ->
                GroupCard(itemScope = itemScope, contentPadding = PaddingValues(0.dp)) {
                    FlowRow(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        toggleableQuickFilters.forEach { quickFilter ->
                            val isChecked = quickFilter.settingsKey in enabledLibraryQuickFilterKeys
                            FilterChip(
                                selected = isChecked,
                                onClick = { onLibraryQuickFilterToggle(quickFilter) },
                                label = { Text(stringResource(quickFilter.titleRes)) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = quickFilter.settingsIcon(),
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

private fun LibraryQuickFilter.settingsIcon(): ImageVector = when (this) {
    LibraryQuickFilter.PASSWORDS -> Icons.Default.Key
    LibraryQuickFilter.TOTP -> Icons.Default.Pin
    LibraryQuickFilter.ALL -> Icons.Default.Key
}
