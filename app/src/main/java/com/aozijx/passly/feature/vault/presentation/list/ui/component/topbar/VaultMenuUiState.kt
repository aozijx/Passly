package com.aozijx.passly.feature.vault.presentation.list.ui.component.topbar

import androidx.compose.runtime.Immutable
import com.aozijx.passly.feature.vault.presentation.list.ui.model.VaultSortUiModel

@Immutable
data class VaultMenuUiState(
    val showTotpCode: Boolean,
    val availableCategories: List<String>,
    val selectedCategory: String?,
    val selectedSort: VaultSortUiModel,
)
