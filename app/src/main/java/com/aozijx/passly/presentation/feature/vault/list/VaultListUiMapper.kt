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
import com.aozijx.passly.presentation.ui.vault.list.model.VaultEntryTypeUiModel
import com.aozijx.passly.presentation.ui.vault.list.model.VaultListItemUiModel
import com.aozijx.passly.presentation.ui.vault.list.model.VaultOtpKindUiModel
import com.aozijx.passly.presentation.ui.vault.list.model.VaultOtpUiState
import com.aozijx.passly.presentation.ui.vault.list.model.VaultQuickFilterUiModel
import com.aozijx.passly.presentation.ui.vault.list.model.VaultSortUiModel
import com.aozijx.passly.presentation.ui.vault.list.model.VaultSwipeActionUiModel

internal fun EntryListItem.toUiModel() = VaultListItemUiModel(
    id = id.value,
    entryType = VaultEntryTypeUiModel.valueOf(entryType.name),
    title = title,
    username = username,
    category = tags.firstOrNull { it.isNotBlank() }?.trim(),
    favorite = favorite,
    associatedDomain = associatedDomain,
    associatedAppPackage = associatedAppPackage,
    iconCustomPath = iconCustomPath,
    hasPassword = hasPassword,
    hasOtp = hasOtp,
    otpKind = otpType?.let { if (it == OtpType.STEAM) VaultOtpKindUiModel.STEAM else VaultOtpKindUiModel.STANDARD },
    otpPreview = otpPreview,
)

internal fun OtpCodeState.toUiModel() = VaultOtpUiState(code, progress, isLoading, error != null)

internal fun LibraryQuickFilter.toUiModel() = VaultQuickFilterUiModel.valueOf(name)
internal fun VaultQuickFilterUiModel.toFeatureModel() = LibraryQuickFilter.valueOf(name)

internal fun EntrySort.toUiModel() = when (field) {
    EntrySortField.TITLE -> VaultSortUiModel.TITLE
    EntrySortField.CREATED_AT -> VaultSortUiModel.CREATED_AT
    EntrySortField.UPDATED_AT -> VaultSortUiModel.UPDATED_AT
    EntrySortField.USAGE_FREQUENCY -> VaultSortUiModel.USAGE_FREQUENCY
    else -> VaultSortUiModel.DEFAULT
}

internal fun VaultSortUiModel.toFeatureModel() = when (this) {
    VaultSortUiModel.DEFAULT -> EntrySort.DEFAULT
    VaultSortUiModel.TITLE -> EntrySort.presets().first { it.field == EntrySortField.TITLE }
    VaultSortUiModel.CREATED_AT -> EntrySort.presets().first { it.field == EntrySortField.CREATED_AT }
    VaultSortUiModel.UPDATED_AT -> EntrySort.presets().first { it.field == EntrySortField.UPDATED_AT }
    VaultSortUiModel.USAGE_FREQUENCY -> EntrySort.presets().first { it.field == EntrySortField.USAGE_FREQUENCY }
}

internal fun SwipeActionType.toUiModel() = VaultSwipeActionUiModel.valueOf(name)
internal fun VaultSwipeActionUiModel.toFeatureModel() = SwipeActionType.valueOf(name)

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
