package com.aozijx.passly.presentation.feature.vault.list.ui.component.topbar

import androidx.compose.runtime.Immutable
import com.aozijx.passly.presentation.feature.vault.list.ui.model.VaultSortUiModel

@Immutable
data class VaultTopBarUiState(
    val query: String,
    val showTotpCode: Boolean,
    val selectedCategory: String?,
    val selectedSort: VaultSortUiModel,
    val availableCategories: List<String>,
    val collapseOnScroll: Boolean,
    val collapseQuickFilterOnScroll: Boolean,
    val hideSystemBars: Boolean,
)
