package com.aozijx.passly.feature.vault.presentation.list.ui.component.topbar

import androidx.compose.runtime.Immutable
import com.aozijx.passly.feature.vault.presentation.list.VaultCategoriesState
import com.aozijx.passly.feature.vault.presentation.list.VaultCategorySelection
import com.aozijx.passly.feature.vault.presentation.list.VaultSortOption
import com.aozijx.passly.feature.vault.presentation.list.ui.model.VaultSortUiModel

@Immutable
data class VaultMenuUiState(
    val showTotpCode: Boolean,
    val categorySearchQuery: String,
    val matchingCategories: List<String>,
    val categoriesState: VaultCategoriesState,
    val categorySelection: VaultCategorySelection,
    val selectedSort: VaultSortUiModel,
    val sortOptions: List<VaultSortOption>,
)
