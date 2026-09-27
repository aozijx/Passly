package com.aozijx.passly.presentation.feature.vault.list

import com.aozijx.passly.domain.entry.model.otp.OtpType
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.model.query.EntryListItem
import com.aozijx.passly.domain.entry.model.query.EntrySort
import com.aozijx.passly.domain.entry.model.query.EntrySortField
import com.aozijx.passly.domain.settings.model.CardDensity
import com.aozijx.passly.domain.settings.model.EntryCardPresentation
import com.aozijx.passly.domain.settings.model.SwipeActionType
import com.aozijx.passly.feature.vault.model.AddType
import com.aozijx.passly.feature.vault.model.OtpCodeState
import com.aozijx.passly.presentation.shared.entry.EntryTypeUiModel
import com.aozijx.passly.presentation.shared.gesture.SwipeActionUiModel
import com.aozijx.passly.presentation.feature.vault.list.ui.model.VaultAddTypeUiModel
import com.aozijx.passly.presentation.feature.vault.list.ui.model.VaultCardDensityUiModel
import com.aozijx.passly.presentation.feature.vault.list.ui.model.VaultCardPresentationUiModel
import com.aozijx.passly.presentation.feature.vault.list.ui.model.VaultListContentUiModel
import com.aozijx.passly.presentation.feature.vault.list.ui.model.VaultListDialogsUiModel
import com.aozijx.passly.presentation.feature.vault.list.ui.model.VaultListDisplayUiModel
import com.aozijx.passly.presentation.feature.vault.list.ui.model.VaultListItemUiModel
import com.aozijx.passly.presentation.feature.vault.list.ui.model.VaultListLayoutUiModel
import com.aozijx.passly.presentation.feature.vault.list.ui.model.VaultListNavigationUiModel
import com.aozijx.passly.presentation.feature.vault.list.ui.model.VaultListScreenUiModel
import com.aozijx.passly.presentation.feature.vault.list.ui.model.VaultListToolbarUiModel
import com.aozijx.passly.presentation.feature.vault.list.ui.model.VaultOtpKindUiModel
import com.aozijx.passly.presentation.feature.vault.list.ui.model.VaultOtpUiState
import com.aozijx.passly.presentation.feature.vault.list.ui.model.VaultSortOptionUiModel
import com.aozijx.passly.presentation.feature.vault.list.ui.model.VaultSortUiModel

internal fun EntryListItem.toUiModel() = VaultListItemUiModel(
    id = id.value,
    entryType = entryType.toUiModel(),
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
)

internal fun OtpCodeState.toUiModel() = VaultOtpUiState(code, progress, isLoading, error != null)


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
        VaultSortOptionUiModel.TITLE -> EntrySort.presets()
            .first { it.field == EntrySortField.TITLE }

        VaultSortOptionUiModel.CREATED_AT -> EntrySort.presets()
            .first { it.field == EntrySortField.CREATED_AT }

        VaultSortOptionUiModel.UPDATED_AT -> EntrySort.presets()
            .first { it.field == EntrySortField.UPDATED_AT }

        VaultSortOptionUiModel.USAGE_FREQUENCY -> EntrySort.presets()
            .first { it.field == EntrySortField.USAGE_FREQUENCY }
    }
    val wantsDescending = preset.direction.name == "DESC"
    return if (wantsDescending == descending) preset else preset.toggled()
}

internal fun EntryType.toUiModel(): EntryTypeUiModel = when (this) {
    EntryType.ACCOUNT -> EntryTypeUiModel.ACCOUNT
    EntryType.LOGIN -> EntryTypeUiModel.LOGIN
    EntryType.NOTE -> EntryTypeUiModel.NOTE
    EntryType.BANK_CARD -> EntryTypeUiModel.BANK_CARD
    EntryType.ID_CARD -> EntryTypeUiModel.ID_CARD
    EntryType.PASSPORT -> EntryTypeUiModel.PASSPORT
    EntryType.DRIVER_LICENSE -> EntryTypeUiModel.DRIVER_LICENSE
    EntryType.SSH_KEY -> EntryTypeUiModel.SSH_KEY
    EntryType.WIFI -> EntryTypeUiModel.WIFI
    EntryType.PASSKEY -> EntryTypeUiModel.PASSKEY
    EntryType.OTP -> EntryTypeUiModel.OTP
    EntryType.DATABASE_CREDENTIAL -> EntryTypeUiModel.DATABASE_CREDENTIAL
    EntryType.SERVER_CREDENTIAL -> EntryTypeUiModel.SERVER_CREDENTIAL
    EntryType.API_KEY -> EntryTypeUiModel.API_KEY
    EntryType.CRYPTO_WALLET -> EntryTypeUiModel.CRYPTO_WALLET
    EntryType.SEED_PHRASE -> EntryTypeUiModel.SEED_PHRASE
    EntryType.RECOVERY_CODE -> EntryTypeUiModel.RECOVERY_CODE
}

internal fun SwipeActionType.toUiModel(): SwipeActionUiModel = when (this) {
    SwipeActionType.DELETE -> SwipeActionUiModel.DELETE
    SwipeActionType.DETAIL -> SwipeActionUiModel.DETAIL
    SwipeActionType.COPY_PASSWORD -> SwipeActionUiModel.COPY_PASSWORD
    SwipeActionType.COPY_USERNAME -> SwipeActionUiModel.COPY_USERNAME
}

internal fun SwipeActionUiModel.toFeatureModel(): SwipeActionType = when (this) {
    SwipeActionUiModel.DELETE -> SwipeActionType.DELETE
    SwipeActionUiModel.DETAIL -> SwipeActionType.DETAIL
    SwipeActionUiModel.COPY_PASSWORD -> SwipeActionType.COPY_PASSWORD
    SwipeActionUiModel.COPY_USERNAME -> SwipeActionType.COPY_USERNAME
}

internal fun AddType.toUiModel(): VaultAddTypeUiModel = when (this) {
    AddType.PASSWORD -> VaultAddTypeUiModel.PASSWORD
    AddType.TOTP -> VaultAddTypeUiModel.TOTP
    AddType.BANK_CARD -> VaultAddTypeUiModel.BANK_CARD
    AddType.WIFI -> VaultAddTypeUiModel.WIFI
    AddType.SSH_KEY -> VaultAddTypeUiModel.SSH_KEY
    AddType.ID_CARD -> VaultAddTypeUiModel.ID_CARD
    AddType.SEED_PHRASE -> VaultAddTypeUiModel.SEED_PHRASE
    AddType.PASSKEY -> VaultAddTypeUiModel.PASSKEY
    AddType.RECOVERY_CODE -> VaultAddTypeUiModel.RECOVERY_CODE
}

internal fun VaultAddTypeUiModel.toFeatureModel(): AddType = when (this) {
    VaultAddTypeUiModel.PASSWORD -> AddType.PASSWORD
    VaultAddTypeUiModel.TOTP -> AddType.TOTP
    VaultAddTypeUiModel.BANK_CARD -> AddType.BANK_CARD
    VaultAddTypeUiModel.WIFI -> AddType.WIFI
    VaultAddTypeUiModel.SSH_KEY -> AddType.SSH_KEY
    VaultAddTypeUiModel.ID_CARD -> AddType.ID_CARD
    VaultAddTypeUiModel.SEED_PHRASE -> AddType.SEED_PHRASE
    VaultAddTypeUiModel.PASSKEY -> AddType.PASSKEY
    VaultAddTypeUiModel.RECOVERY_CODE -> AddType.RECOVERY_CODE
}

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
) = VaultListScreenUiModel(
    toolbar = VaultListToolbarUiModel(
        searchQuery = searchQuery,
        selectedCategory = selectedCategory,
        selectedSort = selectedSort.toUiModel(),
        isSearchActive = isSearchActive,
        availableCategories = availableCategories,
    ),
    navigation = VaultListNavigationUiModel(
        selectedFilters = selectedFilters.map(AddType::toUiModel).toSet(),
        filterOptions = AddType.allOptions.map(AddType::toUiModel),
    ),
    content = VaultListContentUiModel(
        showTotpCode = showTOTPCode,
        cardPresentations = display.cardPresentations,
        swipeLeftAction = display.swipeLeftAction,
        swipeRightAction = display.swipeRightAction,
        isSwipeEnabled = display.isSwipeEnabled,
    ),
    dialogs = VaultListDialogsUiModel(
        addType = addType?.toUiModel(),
        pendingDelete = pendingDelete?.toUiModel(),
    ),
    layout = VaultListLayoutUiModel(
        collapseTopBarOnScroll = display.collapseTopBarOnScroll,
        collapseQuickFilterBarOnScroll = display.collapseQuickFilterBarOnScroll,
        hideSystemBars = display.hideSystemBars,
    ),
)
