package com.aozijx.passly.presentation.feature.vault.list.ui.component.topbar

import androidx.compose.runtime.Immutable
import com.aozijx.passly.presentation.feature.vault.list.ui.model.VaultSortUiModel

@Immutable
data class VaultMenuUiState(
    val showTotpCode: Boolean,
    val availableCategories: List<String>,
    val selectedCategory: String?,
    val selectedSort: VaultSortUiModel,
)
