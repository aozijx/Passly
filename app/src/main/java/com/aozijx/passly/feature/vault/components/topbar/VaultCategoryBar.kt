package com.aozijx.passly.feature.vault.components.topbar

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Pin
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aozijx.passly.feature.vault.model.VaultTab
import com.aozijx.passly.feature.vault.presentation.titleRes

@Composable
fun VaultCategoryBar(
    modifier: Modifier = Modifier,
    tabs: List<VaultTab>,
    selectedTabIndex: Int,
    onTabSelected: (Int) -> Unit
) {
    if (tabs.size <= 1) return

    val safeIndex = selectedTabIndex.coerceIn(0, tabs.lastIndex)
    LazyRow(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        itemsIndexed(
            items = tabs,
            key = { _, tab -> tab.settingsKey }
        ) { index, tab ->
            val selected = safeIndex == index
            FilterChip(
                selected = selected,
                onClick = { onTabSelected(index) },
                label = {
                    Text(
                        text = stringResource(tab.titleRes),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = tab.icon(),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            )
        }
    }
}

private fun VaultTab.icon(): ImageVector = when (this) {
    VaultTab.ALL -> Icons.Default.Apps
    VaultTab.PASSWORDS -> Icons.Default.Key
    VaultTab.TOTP -> Icons.Default.Pin
}
