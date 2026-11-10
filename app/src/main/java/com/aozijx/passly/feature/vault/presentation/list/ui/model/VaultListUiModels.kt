package com.aozijx.passly.feature.vault.presentation.list.ui.model

import androidx.compose.runtime.Immutable
import com.aozijx.passly.domain.settings.model.SwipeActionType
import com.aozijx.passly.feature.vault.presentation.list.VaultCategoriesState
import com.aozijx.passly.feature.vault.presentation.list.VaultCategorySelection
import com.aozijx.passly.feature.vault.presentation.list.VaultSearchInput
import com.aozijx.passly.feature.vault.presentation.list.VaultSortOption

@Immutable
data class VaultSortUiModel(
    val option: VaultSortOption,
    val descending: Boolean,
)

@Immutable
data class VaultCardPresentationUiModel(
    val entryTypeKey: String,
    val variantKey: String,
    val density: VaultCardDensityUiModel,
    val showIcon: Boolean,
    val showFavorite: Boolean,
    val showSecondaryText: Boolean,
    val showQuickAction: Boolean,
)

enum class VaultCardDensityUiModel { COMPACT, STANDARD, COMFORTABLE }

@Immutable
data class VaultListScreenUiModel(
    val toolbar: VaultListToolbarUiModel,
    val navigation: VaultListNavigationUiModel,
    val content: VaultListContentUiModel,
    val layout: VaultListLayoutUiModel,
)

@Immutable
data class VaultListToolbarUiModel(
    val search: VaultSearchInput,
    val categorySelection: VaultCategorySelection,
    val selectedSort: VaultSortUiModel,
    val sortOptions: List<VaultSortOption>,
    val categoriesState: VaultCategoriesState,
    val categorySearchQuery: String,
    val matchingCategories: List<String>,
)

@Immutable
data class VaultListNavigationUiModel(
    val filters: List<VaultFilterOptionUiModel>,
    val creationOptions: List<VaultAddTypeUiModel>,
    val fabMenuOptions: List<VaultAddTypeUiModel>,
) {
    val allFiltersSelected: Boolean
        get() = filters.none(VaultFilterOptionUiModel::selected)

    val hasActiveFilters: Boolean
        get() = filters.any(VaultFilterOptionUiModel::selected)
}

@Immutable
data class VaultFilterOptionUiModel(
    val type: VaultAddTypeUiModel,
    val selected: Boolean,
)

@Immutable
data class VaultListContentUiModel(
    val showTotpCode: Boolean,
    val isQuickFilterBarAvailable: Boolean,
    val cardPresentations: List<VaultCardPresentationUiModel>,
    val swipeLeftAction: SwipeActionType,
    val swipeRightAction: SwipeActionType,
    val isSwipeEnabled: Boolean,
    val isPullToSearchEnabled: Boolean,
)

@Immutable
data class VaultListLayoutUiModel(
    val collapseTopBarOnScroll: Boolean,
    val collapseQuickFilterBarOnScroll: Boolean,
    val hideSystemBars: Boolean,
)
