package com.aozijx.passly.presentation.ui.vault.list.model

import com.aozijx.passly.presentation.ui.shared.entry.EntryTypeUiModel
import com.aozijx.passly.presentation.ui.shared.gesture.SwipeActionUiModel

data class VaultListItemUiModel(
    val id: String,
    val entryType: EntryTypeUiModel,
    val title: String,
    val username: String,
    val category: String?,
    val favorite: Boolean,
    val associatedDomain: String?,
    val associatedAppPackage: String?,
    val iconName: String?,
    val iconCustomPath: String?,
    val hasPassword: Boolean,
    val hasOtp: Boolean,
    val otpKind: VaultOtpKindUiModel?,
    val otpPreview: String?,
    val events: VaultListItemEventHandler = VaultListItemEventHandler.None,
)

interface VaultListItemEventHandler {
    fun onClick()
    fun onSwipe(action: SwipeActionUiModel)

    data object None : VaultListItemEventHandler {
        override fun onClick() = Unit
        override fun onSwipe(action: SwipeActionUiModel) = Unit
    }
}

enum class VaultOtpKindUiModel { STANDARD, STEAM }

data class VaultOtpUiState(
    val code: String? = null,
    val progress: Float = 0f,
    val isLoading: Boolean = false,
    val hasError: Boolean = false,
)

enum class VaultQuickFilterUiModel { ALL, PASSWORDS, TOTP }

data class VaultSortUiModel(
    val option: VaultSortOptionUiModel,
    val descending: Boolean,
) {
    fun toggled() = copy(descending = !descending)

    companion object {
        val DEFAULT = VaultSortUiModel(VaultSortOptionUiModel.DEFAULT, true)
        val presets = listOf(
            DEFAULT,
            VaultSortUiModel(VaultSortOptionUiModel.TITLE, false),
            VaultSortUiModel(VaultSortOptionUiModel.CREATED_AT, true),
            VaultSortUiModel(VaultSortOptionUiModel.UPDATED_AT, true),
            VaultSortUiModel(VaultSortOptionUiModel.USAGE_FREQUENCY, true),
        )
    }
}

enum class VaultSortOptionUiModel { DEFAULT, TITLE, CREATED_AT, UPDATED_AT, USAGE_FREQUENCY }

enum class VaultAddTypeUiModel {
    PASSWORD, TOTP, BANK_CARD, WIFI, SSH_KEY, ID_CARD, SEED_PHRASE, PASSKEY, RECOVERY_CODE;

    companion object {
        val fabMenuOptions = listOf(TOTP, PASSWORD)
        val allOptions = entries.toList()
    }
}

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

data class VaultListScreenUiModel(
    val searchQuery: String,
    val selectedCategory: String?,
    val selectedQuickFilter: VaultQuickFilterUiModel,
    val selectedSort: VaultSortUiModel,
    val isSearchActive: Boolean,
    val availableCategories: List<String>,
    val visibleQuickFilters: List<VaultQuickFilterUiModel>,
    val showTotpCode: Boolean,
    val addType: VaultAddTypeUiModel?,
    val pendingDelete: VaultListItemUiModel?,
    val display: VaultListDisplayUiModel,
    val isDatabaseInitializing: Boolean,
)

data class VaultListDisplayUiModel(
    val cardPresentations: List<VaultCardPresentationUiModel>,
    val swipeLeftAction: SwipeActionUiModel,
    val swipeRightAction: SwipeActionUiModel,
    val isSwipeEnabled: Boolean,
    val isFabVisible: Boolean,
    val collapseTopBarOnScroll: Boolean,
    val collapseQuickFilterBarOnScroll: Boolean,
    val hideSystemBars: Boolean,
)

interface VaultListScreenEventHandler {
    fun onSettingsClick()
    fun onSearchQueryChanged(query: String)
    fun onSearchToggled(active: Boolean)
    fun onClearCategory()
    fun onToggleTotpVisibility()
    fun onCategorySelected(category: String?)
    fun onSortSelected(sort: VaultSortUiModel)
    fun onQuickFilterSelected(filter: VaultQuickFilterUiModel)
    fun onAddTypeSelected(type: VaultAddTypeUiModel)
    fun onDismissAddType()
    fun onConfirmDelete()
    fun onDismissDelete()
    fun requestAuthentication(onSuccess: () -> Unit)
}
