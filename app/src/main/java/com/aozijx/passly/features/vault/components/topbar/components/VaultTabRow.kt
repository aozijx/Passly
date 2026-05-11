package com.aozijx.passly.features.vault.components.topbar.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.aozijx.passly.features.vault.model.VaultTab

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultTabRow(
    modifier: Modifier = Modifier,
    tabs: List<VaultTab>,
    selectedTab: VaultTab,
    maxTabsWithoutScroll: Int = 4,
    onTabSelected: (VaultTab) -> Unit
) {
    if (tabs.size <= 1) return
    val selectedIndex = tabs.indexOf(selectedTab)
    val safePageIndex = selectedIndex.coerceIn(0, tabs.lastIndex)
    val normalizedMaxTabsWithoutScroll = maxTabsWithoutScroll.coerceAtLeast(2)

    if (tabs.size <= normalizedMaxTabsWithoutScroll) {
        Row(modifier = modifier.fillMaxWidth()) {
            tabs.forEachIndexed { index, tab ->
                Tab(
                    selected = safePageIndex == index,
                    onClick = { onTabSelected(tab) },
                    modifier = Modifier.weight(1f),
                    text = {
                        Text(
                            text = stringResource(tab.titleRes),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = if (selectedTab == tab) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
            }
        }
    } else {
        Row(
            modifier = modifier.horizontalScroll(rememberScrollState())
        ) {
            tabs.forEachIndexed { index, tab ->
                Tab(
                    selected = safePageIndex == index,
                    onClick = { onTabSelected(tab) },
                    text = {
                        Text(
                            text = stringResource(tab.titleRes),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = if (selectedTab == tab) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
            }
        }
    }
}