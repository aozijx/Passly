package com.aozijx.passly.presentation.feature.vault.list.ui.model

import androidx.compose.runtime.Immutable
import com.aozijx.passly.presentation.shared.entry.EntryTypeUiModel
import com.aozijx.passly.presentation.shared.gesture.SwipeActionUiModel
import kotlinx.coroutines.flow.Flow

@Immutable
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
)

enum class VaultOtpKindUiModel { STANDARD, STEAM }

@Immutable
data class VaultOtpUiState(
    val code: String? = null,
    val progress: Float = 0f,
    val isLoading: Boolean = false,
    val hasError: Boolean = false,
)

interface VaultOtpStateProvider {
    fun state(entryId: String): Flow<VaultOtpUiState?>
    fun subscribe(entryId: String)
    fun unsubscribe(entryId: String)
}

@Immutable
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
    val dialogs: VaultListDialogsUiModel,
    val layout: VaultListLayoutUiModel,
)

@Immutable
data class VaultListToolbarUiModel(
    val searchQuery: String,
    val selectedCategory: String?,
    val selectedSort: VaultSortUiModel,
    val isSearchActive: Boolean,
    val availableCategories: List<String>,
)

@Immutable
data class VaultListNavigationUiModel(
    val selectedFilters: Set<VaultAddTypeUiModel>,
    val filterOptions: List<VaultAddTypeUiModel>,
)

@Immutable
data class VaultListContentUiModel(
    val showTotpCode: Boolean,
    val cardPresentations: List<VaultCardPresentationUiModel>,
    val swipeLeftAction: SwipeActionUiModel,
    val swipeRightAction: SwipeActionUiModel,
    val isSwipeEnabled: Boolean,
    val isPullToSearchEnabled: Boolean,
)

@Immutable
data class VaultListDialogsUiModel(
    val addType: VaultAddTypeUiModel?,
    val pendingDelete: VaultListItemUiModel?,
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
    val swipeLeftAction: SwipeActionUiModel,
    val swipeRightAction: SwipeActionUiModel,
    val isSwipeEnabled: Boolean,
    val isPullToSearchEnabled: Boolean,
    val collapseTopBarOnScroll: Boolean,
    val collapseQuickFilterBarOnScroll: Boolean,
    val hideSystemBars: Boolean,
)
