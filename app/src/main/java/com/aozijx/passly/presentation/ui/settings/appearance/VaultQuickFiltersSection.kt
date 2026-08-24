package com.aozijx.passly.presentation.ui.settings.appearance

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
import com.aozijx.passly.presentation.ui.settings.appearance.model.LibraryQuickFilterOptionUiModel
import com.aozijx.passly.presentation.ui.settings.appearance.model.LibraryQuickFilterUiModel

@Composable
fun LibraryQuickFiltersSettingsSection(
    options: List<LibraryQuickFilterOptionUiModel>,
    onLibraryQuickFilterToggle: (LibraryQuickFilterUiModel) -> Unit
) {
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
                        options.forEach { option ->
                            FilterChip(
                                selected = option.selected,
                                onClick = { onLibraryQuickFilterToggle(option.filter) },
                                label = { Text(stringResource(option.filter.titleRes)) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = option.filter.settingsIcon(),
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

private fun LibraryQuickFilterUiModel.settingsIcon(): ImageVector = when (this) {
    LibraryQuickFilterUiModel.PASSWORDS -> Icons.Default.Key
    LibraryQuickFilterUiModel.TOTP -> Icons.Default.Pin
}

private val LibraryQuickFilterUiModel.titleRes: Int
    get() = when (this) {
        LibraryQuickFilterUiModel.PASSWORDS -> R.string.vault_quick_filter_passwords
        LibraryQuickFilterUiModel.TOTP -> R.string.vault_quick_filter_totp
    }
