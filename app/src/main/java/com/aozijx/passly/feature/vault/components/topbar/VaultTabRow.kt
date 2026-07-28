package com.aozijx.passly.feature.vault.components.topbar

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SecondaryScrollableTabRow
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aozijx.passly.feature.vault.model.VaultTab

@Composable
fun VaultTabRow(
    modifier: Modifier = Modifier,
    tabs: List<VaultTab>,
    selectedTabIndex: Int,
    maxTabsWithoutScroll: Int = 4,
    onTabSelected: (Int) -> Unit
) {
    if (tabs.size <= 1) return
    val safeIndex = selectedTabIndex.coerceIn(0, tabs.lastIndex)

    if (tabs.size <= maxTabsWithoutScroll) {
        SecondaryTabRow(
            selectedTabIndex = safeIndex,
            modifier = modifier.fillMaxWidth(),
            containerColor = MaterialTheme.colorScheme.surface,
            divider = {}
        ) {
            tabs.forEachIndexed { index, tab ->
                Tab(
                    selected = safeIndex == index,
                    onClick = { onTabSelected(index) },
                    text = {
                        Text(
                            text = stringResource(tab.titleRes),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = if (safeIndex == index) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
            }
        }
    } else {
        SecondaryScrollableTabRow(
            selectedTabIndex = safeIndex,
            modifier = modifier.fillMaxWidth(),
            containerColor = MaterialTheme.colorScheme.surface,
            edgePadding = 16.dp,
            divider = {}
        ) {
            tabs.forEachIndexed { index, tab ->
                Tab(
                    selected = safeIndex == index,
                    onClick = { onTabSelected(index) },
                    text = {
                        Text(
                            text = stringResource(tab.titleRes),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = if (safeIndex == index) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
            }
        }
    }
}
