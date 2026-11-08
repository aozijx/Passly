package com.aozijx.passly.feature.vault.presentation.list

import com.aozijx.passly.domain.entry.model.otp.OtpType
import com.aozijx.passly.domain.entry.model.query.EntryListItem
import com.aozijx.passly.domain.entry.model.query.EntrySort
import com.aozijx.passly.domain.entry.model.query.EntrySortField
import com.aozijx.passly.domain.entry.model.query.SortDirection
import com.aozijx.passly.domain.settings.model.CardDensity
import com.aozijx.passly.domain.settings.model.EntryCardPresentation
import com.aozijx.passly.feature.vault.model.AddType
import com.aozijx.passly.feature.vault.model.OtpCodeState
import com.aozijx.passly.feature.vault.presentation.list.display.VaultDisplayUiState
import com.aozijx.passly.feature.vault.presentation.list.ui.model.VaultAddTypeUiModel
import com.aozijx.passly.feature.vault.presentation.list.ui.model.VaultCardDensityUiModel
import com.aozijx.passly.feature.vault.presentation.list.ui.model.VaultCardPresentationUiModel
import com.aozijx.passly.feature.vault.presentation.list.ui.model.VaultFilterOptionUiModel
import com.aozijx.passly.feature.vault.presentation.list.ui.model.VaultListContentUiModel
import com.aozijx.passly.feature.vault.presentation.list.model.VaultListItemUiModel
import com.aozijx.passly.feature.vault.presentation.list.ui.model.VaultListLayoutUiModel
import com.aozijx.passly.feature.vault.presentation.list.ui.model.VaultListNavigationUiModel
import com.aozijx.passly.feature.vault.presentation.list.ui.model.VaultListScreenUiModel
import com.aozijx.passly.feature.vault.presentation.list.ui.model.VaultListToolbarUiModel
import com.aozijx.passly.feature.vault.presentation.list.model.VaultOtpKindUiModel
import com.aozijx.passly.feature.vault.presentation.list.model.VaultPasswordCardSecondaryTextUiModel
import com.aozijx.passly.feature.vault.presentation.list.model.VaultPasswordCardSupportingTextUiModel
import com.aozijx.passly.feature.vault.presentation.list.model.VaultOtpUiState
import com.aozijx.passly.feature.vault.presentation.list.ui.model.VaultSortOptionUiModel
import com.aozijx.passly.feature.vault.presentation.list.ui.model.VaultSortUiModel
import com.aozijx.passly.feature.vault.presentation.navigation.VaultEditorDestinationRegistry
import com.aozijx.passly.presentation.shared.components.EntryClassificationInput
import com.aozijx.passly.presentation.shared.components.EntryVisualCategoryClassifier
import com.aozijx.passly.presentation.shared.media.toLocalIconImageModel

internal fun EntryListItem.toUiModel(): VaultListItemUiModel {
    val category = tags.firstOrNull(String::isNotBlank)?.trim()
    val domain = associatedDomain?.takeIf(String::isNotBlank)
    val appPackage = associatedAppPackage?.takeIf(String::isNotBlank)
    return VaultListItemUiModel(
        id = id.value,
        entryType = entryType,
        title = title,
        username = username,
        category = category,
        favorite = favorite,
        associatedDomain = domain,
        associatedAppPackage = appPackage,
        iconName = icon.name,
        iconCustomPath = iconCustomPath,
        iconImageModel = toLocalIconImageModel(iconCustomPath),
        visualCategory =
            EntryVisualCategoryClassifier.classify(
                EntryClassificationInput(
                    entryType = entryType,
                    title = title,
                    username = username,
                    urls = setOfNotNull(domain),
                    domains = setOfNotNull(domain),
                    packageNames = setOfNotNull(appPackage),
                    appNames = setOfNotNull(title.takeIf { appPackage != null && domain == null }),
                ),
            ),
        hasPassword = hasPassword,
        hasOtp = hasOtp,
        otpKind = otpType?.let { if (it == OtpType.STEAM) VaultOtpKindUiModel.STEAM else VaultOtpKindUiModel.STANDARD },
        otpPreview = otpPreview,
        passwordCardSecondaryText =
            when {
                domain != null -> VaultPasswordCardSecondaryTextUiModel.Value(domain)
                appPackage != null -> VaultPasswordCardSecondaryTextUiModel.Value(appPackage)
                category != null -> VaultPasswordCardSecondaryTextUiModel.Value(category)
                else -> VaultPasswordCardSecondaryTextUiModel.EntryTypeLabel(entryType)
            },
        passwordCardSupportingText =
            when {
                favorite -> VaultPasswordCardSupportingTextUiModel.FAVORITE
                domain != null || appPackage != null -> VaultPasswordCardSupportingTextUiModel.AUTOFILL
                else -> VaultPasswordCardSupportingTextUiModel.PROTECTED
            },
    )
}

internal fun OtpCodeState.toUiModel(otpKind: VaultOtpKindUiModel?): VaultOtpUiState =
    when (this) {
        OtpCodeState.Pending -> VaultOtpUiState.Pending
        is OtpCodeState.Ready -> VaultOtpUiState.Ready(code.toDisplayCode(otpKind), progress)
        is OtpCodeState.Failed -> VaultOtpUiState.Failed
    }

private fun String.toDisplayCode(otpKind: VaultOtpKindUiModel?): String =
    when {
        otpKind == VaultOtpKindUiModel.STEAM -> this
        length == 6 -> "${take(3)} ${drop(3)}"
        length == 8 -> "${take(4)} ${drop(4)}"
        else -> chunked(3).joinToString(" ")
    }

internal fun EntrySort.toUiModel() =
    VaultSortUiModel(
        option =
            when (field) {
                EntrySortField.TITLE -> VaultSortOptionUiModel.TITLE

                EntrySortField.CREATED_AT -> VaultSortOptionUiModel.CREATED_AT

                EntrySortField.UPDATED_AT -> VaultSortOptionUiModel.UPDATED_AT

                EntrySortField.LAST_USED_AT -> VaultSortOptionUiModel.LAST_USED_AT

                EntrySortField.USAGE_FREQUENCY -> VaultSortOptionUiModel.USAGE_FREQUENCY

                EntrySortField.ENTRY_TYPE,
                EntrySortField.ID,
                -> throw IllegalArgumentException("Unsupported user-facing sort field: $field")
            },
        descending = direction == SortDirection.DESC,
    )

internal fun VaultSortUiModel.toFeatureModel(): EntrySort {
    val preset =
        when (option) {
            VaultSortOptionUiModel.TITLE -> {
                EntrySort
                    .presets()
                    .first { it.field == EntrySortField.TITLE }
            }

            VaultSortOptionUiModel.CREATED_AT -> {
                EntrySort
                    .presets()
                    .first { it.field == EntrySortField.CREATED_AT }
            }

            VaultSortOptionUiModel.UPDATED_AT -> {
                EntrySort
                    .presets()
                    .first { it.field == EntrySortField.UPDATED_AT }
            }

            VaultSortOptionUiModel.LAST_USED_AT -> {
                EntrySort
                    .presets()
                    .first { it.field == EntrySortField.LAST_USED_AT }
            }

            VaultSortOptionUiModel.USAGE_FREQUENCY -> {
                EntrySort
                    .presets()
                    .first { it.field == EntrySortField.USAGE_FREQUENCY }
            }
        }
    val wantsDescending = preset.direction == SortDirection.DESC
    return if (wantsDescending == descending) preset else preset.toggled()
}

internal fun AddType.toUiModel(): VaultAddTypeUiModel =
    when (this) {
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

internal fun EntryCardPresentation.toUiModel() =
    VaultCardPresentationUiModel(
        entryTypeKey = entryTypeKey,
        variantKey = variantKey,
        density =
            when (density) {
                CardDensity.COMPACT -> VaultCardDensityUiModel.COMPACT
                CardDensity.STANDARD -> VaultCardDensityUiModel.STANDARD
                CardDensity.COMFORTABLE -> VaultCardDensityUiModel.COMFORTABLE
            },
        showIcon = showIcon,
        showFavorite = showFavorite,
        showSecondaryText = showSecondaryText,
        showQuickAction = showQuickAction,
    )

internal fun VaultUiState.toUiModel(display: VaultDisplayUiState.Ready): VaultListScreenUiModel? {
    val sort = selectedSort ?: return null
    val creationOptions = VaultEditorDestinationRegistry.creatableTypes.map(AddType::toUiModel)
    return VaultListScreenUiModel(
        toolbar =
            VaultListToolbarUiModel(
                searchQuery = searchQuery,
                selectedCategory = selectedCategory,
                selectedSort = sort.toUiModel(),
                isSearchActive = isSearchActive,
                categoriesState = categories,
                categorySearchQuery = categorySearchQuery,
                matchingCategories =
                    ((categories as? VaultCategoriesState.Ready)?.values ?: emptyList()).filter { category ->
                        categorySearchQuery.isBlank() || category.contains(categorySearchQuery.trim(), ignoreCase = true)
                    },
            ),
        navigation =
            VaultListNavigationUiModel(
                filters =
                    AddType.allOptions.map { type ->
                        VaultFilterOptionUiModel(
                            type = type.toUiModel(),
                            selected = type in selectedFilters,
                        )
                    },
                creationOptions = creationOptions,
                fabMenuOptions = VaultAddTypeUiModel.fabMenuOptions.filter(creationOptions::contains),
            ),
        content =
            VaultListContentUiModel(
                showTotpCode = showTOTPCode,
                isQuickFilterBarAvailable = !isSearchActive && selectedCategory == null,
                cardPresentations =
                    display.style.entryCardPresentations.map(EntryCardPresentation::toUiModel),
                swipeLeftAction = display.interaction.swipeLeftAction,
                swipeRightAction = display.interaction.swipeRightAction,
                isSwipeEnabled = display.interaction.isSwipeEnabled,
                isPullToSearchEnabled = display.interaction.isPullToSearchEnabled,
            ),
        layout =
            VaultListLayoutUiModel(
                collapseTopBarOnScroll = display.layout.collapseTopBarOnScroll,
                collapseQuickFilterBarOnScroll = display.layout.collapseQuickFilterBarOnScroll,
                hideSystemBars = display.layout.hideSystemBars,
            ),
    )
}
