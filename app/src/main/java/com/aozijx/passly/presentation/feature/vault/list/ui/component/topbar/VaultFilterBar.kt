package com.aozijx.passly.presentation.feature.vault.list.ui.component.topbar

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aozijx.passly.R
import com.aozijx.passly.presentation.feature.vault.list.ui.model.VaultAddTypeUiModel
import com.aozijx.passly.presentation.feature.vault.list.ui.model.icon
import com.aozijx.passly.presentation.feature.vault.list.ui.model.labelRes

@Composable
fun VaultFilterBar(
    modifier: Modifier = Modifier,
    filters: List<VaultAddTypeUiModel>,
    selectedFilters: Set<VaultAddTypeUiModel>,
    onFilterToggled: (VaultAddTypeUiModel?) -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
    ) {
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item(key = "all") {
                FilterChip(
                    selected = selectedFilters.isEmpty(),
                    onClick = { onFilterToggled(null) },
                    label = {
                        Text(
                            text = stringResource(R.string.tab_all),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = if (selectedFilters.isEmpty()) {
                                FontWeight.SemiBold
                            } else {
                                FontWeight.Normal
                            },
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Apps,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                    },
                )
            }
            items(filters, key = VaultAddTypeUiModel::name) { filter ->
                val selected = filter in selectedFilters
                FilterChip(
                    selected = selected,
                    onClick = { onFilterToggled(filter) },
                    label = {
                        Text(
                            text = stringResource(filter.labelRes),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = filter.icon(),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                    },
                )
            }
        }
    }
}
