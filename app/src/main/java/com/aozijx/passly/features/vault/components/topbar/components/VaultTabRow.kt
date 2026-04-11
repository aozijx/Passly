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
import com.aozijx.passly.R
import com.aozijx.passly.core.designsystem.model.VaultTab

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultTabRow(
    selectedTab: VaultTab, onTabSelected: (VaultTab) -> Unit, modifier: Modifier = Modifier
) {
    SecondaryTabRow(
        selectedTabIndex = selectedTab.ordinal,
        containerColor = MaterialTheme.colorScheme.surface,
        indicator = {
            TabRowDefaults.SecondaryIndicator(
                Modifier.tabIndicatorOffset(selectedTab.ordinal),
                color = MaterialTheme.colorScheme.primary
            )
        },
        modifier = modifier
    ) {
        VaultTab.entries.forEach { tab ->
            Tab(selected = selectedTab == tab, onClick = { onTabSelected(tab) }, text = {
                Text(
                    text = stringResource(
                        when (tab) {
                            VaultTab.ALL -> R.string.vault_tab_all
                            VaultTab.PASSWORDS -> R.string.vault_tab_passwords
                            VaultTab.TOTP -> R.string.vault_tab_totp
                        }
                    ),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = if (selectedTab == tab) FontWeight.Bold else FontWeight.Normal
                )
            })
        }
    }
}
