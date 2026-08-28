package com.aozijx.passly.presentation.feature.vault.list

import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.model.otp.OtpType
import com.aozijx.passly.domain.entry.model.query.EntryListItem
import com.aozijx.passly.domain.entry.model.query.EntrySort
import com.aozijx.passly.domain.entry.model.query.EntrySortField
import com.aozijx.passly.domain.settings.model.CardDensity
import com.aozijx.passly.domain.settings.model.EntryCardPresentation
import com.aozijx.passly.domain.settings.model.LibraryQuickFilter
import com.aozijx.passly.domain.settings.model.SwipeActionType
import com.aozijx.passly.feature.vault.model.AddType
import com.aozijx.passly.feature.vault.model.OtpCodeState
import com.aozijx.passly.presentation.ui.vault.list.model.VaultAddTypeUiModel
import com.aozijx.passly.presentation.ui.vault.list.model.VaultCardDensityUiModel
import com.aozijx.passly.presentation.ui.vault.list.model.VaultCardPresentationUiModel
import com.aozijx.passly.presentation.ui.shared.entry.EntryTypeUiModel
import com.aozijx.passly.presentation.ui.vault.list.model.VaultListItemUiModel
import com.aozijx.passly.presentation.ui.vault.list.model.VaultListItemEventHandler
import com.aozijx.passly.presentation.ui.vault.list.model.VaultListDisplayUiModel
import com.aozijx.passly.presentation.ui.vault.list.model.VaultListScreenUiModel
import com.aozijx.passly.presentation.ui.vault.list.model.VaultOtpKindUiModel
import com.aozijx.passly.presentation.ui.vault.list.model.VaultOtpUiState
import com.aozijx.passly.presentation.ui.vault.list.model.VaultQuickFilterUiModel
import com.aozijx.passly.presentation.ui.vault.list.model.VaultSortUiModel
import com.aozijx.passly.presentation.ui.vault.list.model.VaultSortOptionUiModel
import com.aozijx.passly.presentation.ui.shared.gesture.SwipeActionUiModel

internal fun EntryListItem.toUiModel(
    events: VaultListItemEventHandler = VaultListItemEventHandler.None,
) = VaultListItemUiModel(
    id = id.value,
    entryType = EntryTypeUiModel.valueOf(entryType.name),
    title = title,
    username = username,
    category = tags.firstOrNull { it.isNotBlank() }?.trim(),
    favorite = favorite,
    associatedDomain = associatedDomain,
    associatedAppPackage = associatedAppPackage,
    iconName = icon.name,
    iconCustomPath = iconCustomPath,
    hasPassword = hasPassword,
    hasOtp = hasOtp,
    otpKind = otpType?.let { if (it == OtpType.STEAM) VaultOtpKindUiModel.STEAM else VaultOtpKindUiModel.STANDARD },
    otpPreview = otpPreview,
    events = events,
)

internal fun OtpCodeState.toUiModel() = VaultOtpUiState(code, progress, isLoading, error != null)

internal fun LibraryQuickFilter.toUiModel() = VaultQuickFilterUiModel.valueOf(name)
internal fun VaultQuickFilterUiModel.toFeatureModel() = LibraryQuickFilter.valueOf(name)

internal fun EntrySort.toUiModel() = VaultSortUiModel(
    option = when (field) {
        EntrySortField.TITLE -> VaultSortOptionUiModel.TITLE
        EntrySortField.CREATED_AT -> VaultSortOptionUiModel.CREATED_AT
        EntrySortField.UPDATED_AT -> VaultSortOptionUiModel.UPDATED_AT
        EntrySortField.USAGE_FREQUENCY -> VaultSortOptionUiModel.USAGE_FREQUENCY
        else -> VaultSortOptionUiModel.DEFAULT
    },
    descending = direction.name == "DESC",
)

internal fun VaultSortUiModel.toFeatureModel(): EntrySort {
    val preset = when (option) {
        VaultSortOptionUiModel.DEFAULT -> EntrySort.DEFAULT
        VaultSortOptionUiModel.TITLE -> EntrySort.presets().first { it.field == EntrySortField.TITLE }
        VaultSortOptionUiModel.CREATED_AT -> EntrySort.presets().first { it.field == EntrySortField.CREATED_AT }
        VaultSortOptionUiModel.UPDATED_AT -> EntrySort.presets().first { it.field == EntrySortField.UPDATED_AT }
        VaultSortOptionUiModel.USAGE_FREQUENCY -> EntrySort.presets().first { it.field == EntrySortField.USAGE_FREQUENCY }
    }
    val wantsDescending = preset.direction.name == "DESC"
    return if (wantsDescending == descending) preset else preset.toggled()
}

internal fun SwipeActionType.toUiModel() = SwipeActionUiModel.valueOf(name)
internal fun SwipeActionUiModel.toFeatureModel() = SwipeActionType.valueOf(name)

internal fun AddType.toUiModel() = VaultAddTypeUiModel.valueOf(name)
internal fun VaultAddTypeUiModel.toFeatureModel() = AddType.valueOf(name)

internal fun EntryCardPresentation.toUiModel() = VaultCardPresentationUiModel(
    entryTypeKey = entryTypeKey,
    variantKey = variantKey,
    density = when (density) {
        CardDensity.COMPACT -> VaultCardDensityUiModel.COMPACT
        CardDensity.STANDARD -> VaultCardDensityUiModel.STANDARD
        CardDensity.COMFORTABLE -> VaultCardDensityUiModel.COMFORTABLE
    },
    showIcon = showIcon,
    showFavorite = showFavorite,
    showSecondaryText = showSecondaryText,
    showQuickAction = showQuickAction,
)

internal fun VaultUiState.toUiModel(
    display: VaultListDisplayUiModel,
    isDatabaseInitializing: Boolean,
) = VaultListScreenUiModel(
    searchQuery = searchQuery,
    selectedCategory = selectedCategory,
    selectedQuickFilter = selectedQuickFilter.toUiModel(),
    selectedSort = selectedSort.toUiModel(),
    isSearchActive = isSearchActive,
    availableCategories = availableCategories,
    visibleQuickFilters = visibleQuickFilters.map(LibraryQuickFilter::toUiModel),
    showTotpCode = showTOTPCode,
    addType = addType?.toUiModel(),
    pendingDelete = pendingDelete?.toUiModel(),
    display = display,
    isDatabaseInitializing = isDatabaseInitializing,
)
