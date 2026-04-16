package com.aozijx.passly.features.vault.components.topbar.components

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.aozijx.passly.features.vault.model.VaultTab

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultTabRow(
    tabs: List<VaultTab>,
    selectedTab: VaultTab,
    currentPageIndex: Int,
    onTabSelected: (VaultTab) -> Unit,
    modifier: Modifier = Modifier
) {
    if (tabs.isEmpty()) return
    val safePageIndex = currentPageIndex.coerceIn(0, tabs.lastIndex)
    SecondaryTabRow(
        selectedTabIndex = safePageIndex,
        containerColor = MaterialTheme.colorScheme.surface,
        indicator = {
            TabRowDefaults.SecondaryIndicator(
                Modifier.tabIndicatorOffset(safePageIndex),
                color = MaterialTheme.colorScheme.primary
            )
        },
        modifier = modifier
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