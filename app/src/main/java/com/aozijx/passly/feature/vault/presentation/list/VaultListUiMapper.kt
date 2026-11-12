package com.aozijx.passly.feature.vault.presentation.list

import com.aozijx.passly.domain.entry.model.otp.OtpType
import com.aozijx.passly.domain.entry.model.query.EntryListItem
import com.aozijx.passly.domain.entry.model.query.EntrySort
import com.aozijx.passly.domain.entry.model.query.EntrySortField
import com.aozijx.passly.domain.entry.model.query.SortDirection
import com.aozijx.passly.domain.settings.model.CardDensity
import com.aozijx.passly.domain.settings.model.EntryCardPresentation
import com.aozijx.passly.feature.vault.model.AddType
import com.aozijx.passly.feature.vault.otp.presentation.OtpPresentation
import com.aozijx.passly.feature.vault.presentation.editor.VaultEditorCapabilities
import com.aozijx.passly.feature.vault.presentation.list.model.VaultListItemCategoryLabel
import com.aozijx.passly.feature.vault.presentation.list.model.VaultListItemUiModel
import com.aozijx.passly.feature.vault.presentation.list.model.VaultPasswordCardSecondaryTextUiModel
import com.aozijx.passly.feature.vault.presentation.list.model.VaultPasswordCardSupportingTextUiModel
import com.aozijx.passly.feature.vault.presentation.list.ui.model.VaultAddTypePresentation
import com.aozijx.passly.feature.vault.presentation.list.ui.model.VaultCardDensityUiModel
import com.aozijx.passly.feature.vault.presentation.list.ui.model.VaultCardPresentationUiModel
import com.aozijx.passly.feature.vault.presentation.list.ui.model.VaultFilterOptionUiModel
import com.aozijx.passly.feature.vault.presentation.list.ui.model.VaultListContentUiModel
import com.aozijx.passly.feature.vault.presentation.list.ui.model.VaultListLayoutUiModel
import com.aozijx.passly.feature.vault.presentation.list.ui.model.VaultListNavigationUiModel
import com.aozijx.passly.feature.vault.presentation.list.ui.model.VaultListScreenUiModel
import com.aozijx.passly.feature.vault.presentation.list.ui.model.VaultListToolbarUiModel
import com.aozijx.passly.feature.vault.presentation.list.ui.model.VaultSortUiModel
import com.aozijx.passly.presentation.shared.entry.labelRes
import com.aozijx.passly.presentation.shared.entryvisual.EntryClassificationInput
import com.aozijx.passly.presentation.shared.entryvisual.EntryVisualCategoryClassifier
import com.aozijx.passly.presentation.shared.entryvisual.vaultItemIconUiModel

internal fun EntryListItem.toUiModel(): VaultListItemUiModel {
    val category = tags.firstOrNull(String::isNotBlank)?.trim()
    val domain = associatedDomain?.takeIf(String::isNotBlank)
    val appPackage = associatedAppPackage?.takeIf(String::isNotBlank)
    return VaultListItemUiModel(
        id = id.value,
        entryType = entryType,
        title = title,
        username = username,
        categoryLabel =
            category?.let(VaultListItemCategoryLabel::Tag)
                ?: VaultListItemCategoryLabel.EntryTypeName(entryType.labelRes),
        favorite = favorite,
        icon =
            vaultItemIconUiModel(
                iconName = icon.name,
                customImagePath = iconCustomPath,
                associatedAppPackage = appPackage,
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
            ),
        hasPassword = hasPassword,
        otpPresentation =
            if (hasOtp) {
                OtpPresentation.Applicable(requireNotNull(otpType) { "OTP entry is missing its OTP type" })
            } else {
                OtpPresentation.NotApplicable
            },
        passwordCardSecondaryText =
            when {
                domain != null -> VaultPasswordCardSecondaryTextUiModel.Value(domain)
                appPackage != null -> VaultPasswordCardSecondaryTextUiModel.Value(appPackage)
                category != null -> VaultPasswordCardSecondaryTextUiModel.Value(category)
                else -> VaultPasswordCardSecondaryTextUiModel.EntryTypeLabel(entryType.labelRes)
            },
        passwordCardSupportingText =
            when {
                favorite -> VaultPasswordCardSupportingTextUiModel.FAVORITE
                domain != null || appPackage != null -> VaultPasswordCardSupportingTextUiModel.AUTOFILL
                else -> VaultPasswordCardSupportingTextUiModel.PROTECTED
            },
    )
}

internal fun EntrySort.toUiModel() =
    VaultSortUiModel(
        option = field.toVaultSortOption(),
        descending = direction == SortDirection.DESC,
    )

private fun EntrySortField.toVaultSortOption(): VaultSortOption =
    when (this) {
        EntrySortField.TITLE -> {
            VaultSortOption.TITLE
        }

        EntrySortField.CREATED_AT -> {
            VaultSortOption.CREATED_AT
        }

        EntrySortField.UPDATED_AT -> {
            VaultSortOption.UPDATED_AT
        }

        EntrySortField.LAST_USED_AT -> {
            VaultSortOption.LAST_USED_AT
        }

        EntrySortField.USAGE_FREQUENCY -> {
            VaultSortOption.USAGE_FREQUENCY
        }

        EntrySortField.ENTRY_TYPE, EntrySortField.ID -> {
            throw IllegalArgumentException("Unsupported user-facing sort field: $this")
        }
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

internal fun VaultUiState.toUiModel(display: VaultDisplayUiState.Ready): VaultListScreenUiModel {
    val creationOptions = VaultEditorCapabilities.creatableTypes
    return VaultListScreenUiModel(
        toolbar =
            VaultListToolbarUiModel(
                search = search,
                categorySelection = categorySelection,
                selectedSort = display.sort.toUiModel(),
                sortOptions = EntrySort.presets().map { it.field.toVaultSortOption() },
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
                            type = type,
                            selected = type in selectedFilters,
                        )
                    },
                creationOptions = creationOptions,
                fabMenuOptions = VaultAddTypePresentation.fabMenuOptions.filter(creationOptions::contains),
            ),
        content =
            VaultListContentUiModel(
                showTotpCode = showTotpCode,
                isQuickFilterBarAvailable = !search.isActive && categorySelection == VaultCategorySelection.All,
                cardPresentations = display.entryCardPresentations.map(EntryCardPresentation::toUiModel),
                swipeLeftAction = display.swipeLeftAction,
                swipeRightAction = display.swipeRightAction,
                isSwipeEnabled = display.isSwipeEnabled,
                isPullToSearchEnabled = display.isPullToSearchEnabled,
            ),
        layout =
            VaultListLayoutUiModel(
                collapseTopBarOnScroll = display.collapseTopBarOnScroll,
                collapseQuickFilterBarOnScroll = display.collapseQuickFilterBarOnScroll,
                hideSystemBars = display.hideSystemBars,
            ),
    )
}
