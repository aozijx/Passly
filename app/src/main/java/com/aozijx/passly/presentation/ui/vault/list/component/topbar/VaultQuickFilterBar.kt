package com.aozijx.passly.presentation.ui.vault.list.component.topbar

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
import com.aozijx.passly.R
import com.aozijx.passly.presentation.ui.vault.list.model.VaultQuickFilterUiModel

@Composable
fun LibraryQuickFilterBar(
    modifier: Modifier = Modifier,
    quickFilters: List<VaultQuickFilterUiModel>,
    selectedQuickFilterIndex: Int,
    onQuickFilterSelected: (Int) -> Unit
) {
    if (quickFilters.size <= 1) return

    val safeIndex = selectedQuickFilterIndex.coerceIn(0, quickFilters.lastIndex)
    LazyRow(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        itemsIndexed(
            items = quickFilters,
            key = { _, quickFilter -> quickFilter.name }
        ) { index, quickFilter ->
            val selected = safeIndex == index
            FilterChip(
                selected = selected,
                onClick = { onQuickFilterSelected(index) },
                label = {
                    Text(
                        text = stringResource(quickFilter.titleRes),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = quickFilter.icon(),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            )
        }
    }
}

private fun VaultQuickFilterUiModel.icon(): ImageVector = when (this) {
    VaultQuickFilterUiModel.ALL -> Icons.Default.Apps
    VaultQuickFilterUiModel.PASSWORDS -> Icons.Default.Key
    VaultQuickFilterUiModel.TOTP -> Icons.Default.Pin
}

private val VaultQuickFilterUiModel.titleRes: Int
    get() = when (this) {
        VaultQuickFilterUiModel.ALL -> R.string.tab_all
        VaultQuickFilterUiModel.PASSWORDS -> R.string.vault_quick_filter_passwords
        VaultQuickFilterUiModel.TOTP -> R.string.vault_quick_filter_totp
    }
