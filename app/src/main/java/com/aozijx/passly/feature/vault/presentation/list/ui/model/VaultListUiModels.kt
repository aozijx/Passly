package com.aozijx.passly.feature.vault.presentation.list.ui.model

import androidx.compose.runtime.Immutable
import com.aozijx.passly.domain.settings.model.SwipeActionType
import com.aozijx.passly.feature.vault.presentation.list.VaultCategoriesState

@Immutable
data class VaultSortUiModel(
    val option: VaultSortOptionUiModel,
    val descending: Boolean,
) {
    fun toggled() = copy(descending = !descending)

    companion object {
        val presets =
            listOf(
                VaultSortUiModel(VaultSortOptionUiModel.CREATED_AT, true),
                VaultSortUiModel(VaultSortOptionUiModel.TITLE, false),
                VaultSortUiModel(VaultSortOptionUiModel.UPDATED_AT, true),
                VaultSortUiModel(VaultSortOptionUiModel.LAST_USED_AT, true),
                VaultSortUiModel(VaultSortOptionUiModel.USAGE_FREQUENCY, true),
            )
    }
}

enum class VaultSortOptionUiModel { TITLE, CREATED_AT, UPDATED_AT, LAST_USED_AT, USAGE_FREQUENCY }

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
    val searchQuery: String,
    val selectedCategory: String?,
    val selectedSort: VaultSortUiModel,
    val isSearchActive: Boolean,
    val categoriesState: VaultCategoriesState,
    val categorySearchQuery: String,
    val matchingCategories: List<String>,
)

@Immutable
data class VaultListNavigationUiModel(
    val filters: List<VaultFilterOptionUiModel>,
    val creationOptions: List<VaultAddTypeUiModel>,
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

@Immutable
data class VaultListDisplayUiModel(
    val cardPresentations: List<VaultCardPresentationUiModel>,
    val swipeLeftAction: SwipeActionType,
    val swipeRightAction: SwipeActionType,
    val isSwipeEnabled: Boolean,
    val isPullToSearchEnabled: Boolean,
    val collapseTopBarOnScroll: Boolean,
    val collapseQuickFilterBarOnScroll: Boolean,
    val hideSystemBars: Boolean,
)
