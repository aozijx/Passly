package com.aozijx.passly.feature.vault.presentation.list

import com.aozijx.passly.R
import com.aozijx.passly.domain.entry.model.EntryAssociations
import com.aozijx.passly.domain.entry.model.EntryIcon
import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.entry.model.EntryIdentity
import com.aozijx.passly.domain.entry.model.EntryProfile
import com.aozijx.passly.domain.entry.model.EntryTimestamps
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.model.otp.OtpType
import com.aozijx.passly.domain.entry.model.query.EntryCapabilities
import com.aozijx.passly.domain.entry.model.query.EntryCapability
import com.aozijx.passly.domain.entry.model.query.EntryListItem
import com.aozijx.passly.domain.entry.model.query.EntrySort
import com.aozijx.passly.domain.entry.model.query.EntrySortField
import com.aozijx.passly.domain.entry.model.query.SortDirection
import com.aozijx.passly.domain.settings.model.SwipeActionType
import com.aozijx.passly.feature.vault.otp.presentation.OtpPresentation
import com.aozijx.passly.feature.vault.model.AddType
import com.aozijx.passly.feature.vault.presentation.list.VaultSortOption
import com.aozijx.passly.feature.vault.presentation.list.model.VaultListItemCategoryLabel
import com.aozijx.passly.feature.vault.presentation.list.model.VaultPasswordCardSecondaryTextUiModel
import com.aozijx.passly.feature.vault.presentation.list.model.VaultPasswordCardSupportingTextUiModel
import com.aozijx.passly.presentation.shared.entryvisual.EntryVisualCategory
import com.aozijx.passly.presentation.shared.entryvisual.VaultItemAssociatedAppUiState
import com.aozijx.passly.presentation.shared.entryvisual.VaultItemIconGlyphUiState
import com.aozijx.passly.presentation.shared.entryvisual.VaultItemIconImageUiState
import com.aozijx.passly.presentation.shared.media.LocalIconImageModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VaultListUiMapperTest {
    @Test
    fun creationOptionsKeepTheirFeatureMeaningAndStableOrder() {
        val model = VaultUiState().toUiModel(display())
        val menuTypes = model.navigation.creationOptions
        assertEquals(
            listOf(
                AddType.PASSWORD,
                AddType.TOTP,
                AddType.BANK_CARD,
                AddType.WIFI,
            ),
            menuTypes,
        )
        assertEquals(
            listOf(AddType.TOTP, AddType.PASSWORD),
            model.navigation.fabMenuOptions,
        )
    }

    @Test
    fun allEntryTypesAreAvailableAsFilters() {
        val state = VaultUiState(selectedFilters = AddType.entries.toSet())
        val model = state.toUiModel(display())
        assertEquals(AddType.entries.toList(), model.navigation.filters.map { it.type })
        assertTrue(model.navigation.filters.all { it.selected })
        assertTrue(model.navigation.allFiltersSelected.not())
        assertTrue(model.navigation.hasActiveFilters)
    }

    @Test
    fun selectedSortIsProjectedFromThePersistedSettingsOwner() {
        val model = VaultUiState().toUiModel(display())

        assertEquals(VaultSortOption.CREATED_AT, model.toolbar.selectedSort.option)
        assertTrue(model.toolbar.selectedSort.descending)
    }

    @Test
    fun sortOptionsFollowTheDomainPresetOrderWithoutDuplicates() {
        assertEquals(
            listOf(
                VaultSortOption.CREATED_AT,
                VaultSortOption.TITLE,
                VaultSortOption.UPDATED_AT,
                VaultSortOption.LAST_USED_AT,
                VaultSortOption.USAGE_FREQUENCY,
            ),
            VaultUiState().toUiModel(display()).toolbar.sortOptions,
        )
    }

    @Test
    fun createdAtDescendingSortMapsToNewestFirst() {
        val ui = createdAtDescendingSort().toUiModel()

        assertEquals(VaultSortOption.CREATED_AT, ui.option)
        assertTrue(ui.descending)
    }

    @Test
    fun categorySearchMapsTrimmedCaseInsensitiveMatchesAndPreservesQuery() {
        val model =
            VaultUiState(
                categories = VaultCategoriesState.Ready(listOf("Personal", "Work", "Work Account", "social")),
                categorySearchQuery = " WORk ",
            ).toUiModel(display())

        assertEquals(" WORk ", model.toolbar.categorySearchQuery)
        assertEquals(listOf("Work", "Work Account"), model.toolbar.matchingCategories)
        assertEquals(
            VaultCategoriesState.Ready(listOf("Personal", "Work", "Work Account", "social")),
            model.toolbar.categoriesState,
        )
    }

    @Test
    fun categoryFailureStateIsPreservedForRetryPresentation() {
        val model =
            VaultUiState(
                categories = VaultCategoriesState.Failed,
            ).toUiModel(display())

        assertEquals(VaultCategoriesState.Failed, model.toolbar.categoriesState)
        assertTrue(model.toolbar.matchingCategories.isEmpty())
    }

    private fun display() =
        VaultDisplayUiState.Ready(
            hideSystemBars = false,
            collapseTopBarOnScroll = false,
            collapseQuickFilterBarOnScroll = false,
            entryCardPresentations = emptyList(),
            isSwipeEnabled = true,
            isPullToSearchEnabled = false,
            swipeLeftAction = SwipeActionType.DELETE,
            swipeRightAction = SwipeActionType.COPY_PASSWORD,
            sort = createdAtDescendingSort(),
        )

    private fun createdAtDescendingSort() =
        EntrySort(
            field = EntrySortField.CREATED_AT,
            direction = SortDirection.DESC,
        )

    @Test
    fun entryMappingPreservesEveryRenderedValue() {
        val item =
            EntryListItem(
                identity =
                    EntryIdentity(
                        id = EntryId("entry-1"),
                        type = EntryType.LOGIN,
                        timestamps = EntryTimestamps(1L),
                    ),
                profile =
                    EntryProfile(
                        title = "Mail",
                        username = "user@example.com",
                        tags = linkedSetOf("  Personal  "),
                        associations = EntryAssociations(primaryUrl = "example.com", applicationIds = setOf("com.example")),
                        icon = EntryIcon(customReference = "icons/mail.png"),
                        favorite = true,
                    ),
                capabilities = EntryCapabilities(setOf(EntryCapability.PASSWORD, EntryCapability.OTP)),
                otpType = OtpType.STEAM,
            )

        val ui = item.toUiModel()

        assertEquals("entry-1", ui.id)
        assertEquals("Mail", ui.title)
        assertEquals("user@example.com", ui.username)
        assertEquals(VaultListItemCategoryLabel.Tag("Personal"), ui.categoryLabel)
        assertEquals(VaultItemAssociatedAppUiState.Package("com.example"), ui.icon.associatedApp)
        assertEquals(
            VaultItemIconImageUiState.Custom(LocalIconImageModel("file://icons/mail.png")),
            ui.icon.image,
        )
        assertEquals(VaultItemIconGlyphUiState.CategoryFallback, ui.icon.glyph)
        assertEquals(EntryVisualCategory.EMAIL, ui.icon.visualCategory)
        assertTrue(ui.favorite)
        assertTrue(ui.hasPassword)
        assertEquals(OtpPresentation.Applicable(OtpType.STEAM), ui.otpPresentation)
        assertEquals(
            VaultPasswordCardSecondaryTextUiModel.Value("example.com"),
            ui.passwordCardSecondaryText,
        )
        assertEquals(VaultPasswordCardSupportingTextUiModel.FAVORITE, ui.passwordCardSupportingText)
    }

    @Test
    fun blankAssociatedDomainIsAbsentAcrossVaultListProjection() {
        val item =
            EntryListItem(
                identity =
                    EntryIdentity(
                        id = EntryId("entry-blank-domain"),
                        type = EntryType.LOGIN,
                        timestamps = EntryTimestamps(1L),
                    ),
                profile =
                    EntryProfile(
                        title = "Slack",
                        associations = EntryAssociations(primaryUrl = " ", applicationIds = setOf("com.slack")),
                    ),
            )
        val withoutDomain = item.copy(profile = item.profile.copy(associations = item.associations.copy(primaryUrl = null)))

        val ui = item.toUiModel()
        val withoutDomainUi = withoutDomain.toUiModel()

        assertEquals(VaultItemIconImageUiState.Default, ui.icon.image)
        assertEquals(VaultItemIconGlyphUiState.CategoryFallback, ui.icon.glyph)
        assertEquals(VaultItemAssociatedAppUiState.Package("com.slack"), ui.icon.associatedApp)
        assertEquals(withoutDomainUi.icon.visualCategory, ui.icon.visualCategory)
        assertEquals(VaultPasswordCardSecondaryTextUiModel.Value("com.slack"), ui.passwordCardSecondaryText)
        assertEquals(VaultPasswordCardSupportingTextUiModel.AUTOFILL, ui.passwordCardSupportingText)
    }

    @Test
    fun entryTypeNamesAreProjectedToLocalizedResources() {
        val item =
            EntryListItem(
                identity =
                    EntryIdentity(
                        id = EntryId("entry-type-label"),
                        type = EntryType.LOGIN,
                        timestamps = EntryTimestamps(1L),
                    ),
                profile = EntryProfile(title = "Mail"),
            )

        val ui = item.toUiModel()

        assertEquals(VaultListItemCategoryLabel.EntryTypeName(R.string.entry_type_login), ui.categoryLabel)
        assertEquals(
            VaultPasswordCardSecondaryTextUiModel.EntryTypeLabel(R.string.entry_type_login),
            ui.passwordCardSecondaryText,
        )
    }
}
