package com.aozijx.passly.feature.vault.presentation.list

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
import com.aozijx.passly.feature.vault.model.AddType
import com.aozijx.passly.feature.vault.presentation.list.ui.model.VaultAddTypeUiModel
import com.aozijx.passly.feature.vault.presentation.list.ui.model.VaultListDisplayUiModel
import com.aozijx.passly.feature.vault.presentation.list.model.VaultOtpKindUiModel
import com.aozijx.passly.feature.vault.presentation.list.model.VaultPasswordCardSecondaryTextUiModel
import com.aozijx.passly.feature.vault.presentation.list.model.VaultPasswordCardSupportingTextUiModel
import com.aozijx.passly.feature.vault.presentation.list.ui.model.VaultSortOptionUiModel
import com.aozijx.passly.feature.vault.presentation.list.ui.model.VaultSortUiModel
import com.aozijx.passly.presentation.shared.components.EntryVisualCategory
import com.aozijx.passly.presentation.shared.media.LocalIconImageModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VaultListUiMapperTest {
    @Test
    fun creationOptionsAreMappedInStableOrder() {
        val model = requireNotNull(VaultUiState(selectedSort = createdAtDescendingSort()).toUiModel(display()))
        val menuTypes = model.navigation.creationOptions
        assertEquals(
            listOf(
                VaultAddTypeUiModel.PASSWORD,
                VaultAddTypeUiModel.TOTP,
                VaultAddTypeUiModel.BANK_CARD,
                VaultAddTypeUiModel.WIFI,
            ),
            menuTypes,
        )
    }

    @Test
    fun allEntryTypesAreAvailableAsFilters() {
        val state = VaultUiState(selectedSort = createdAtDescendingSort(), selectedFilters = AddType.entries.toSet())
        val model = requireNotNull(state.toUiModel(display()))
        assertEquals(VaultAddTypeUiModel.entries.toList(), model.navigation.filters.map { it.type })
        assertTrue(model.navigation.filters.all { it.selected })
        assertTrue(model.navigation.allFiltersSelected.not())
        assertTrue(model.navigation.hasActiveFilters)
    }

    @Test
    fun vaultDoesNotRenderBeforePersistedSortLoads() {
        assertNull(VaultUiState().toUiModel(display()))
    }

    @Test
    fun sortPresetsContainCreatedAtOnlyOnce() {
        assertEquals(
            1,
            VaultSortUiModel.presets.count { it.option == VaultSortOptionUiModel.CREATED_AT },
        )
    }

    @Test
    fun createdAtDescendingSortMapsToNewestFirst() {
        val ui = createdAtDescendingSort().toUiModel()

        assertEquals(VaultSortOptionUiModel.CREATED_AT, ui.option)
        assertTrue(ui.descending)
    }

    @Test
    fun categorySearchMapsTrimmedCaseInsensitiveMatchesAndPreservesQuery() {
        val model =
            requireNotNull(
                VaultUiState(
                    selectedSort = createdAtDescendingSort(),
                    categories = VaultCategoriesState.Ready(listOf("Personal", "Work", "Work Account", "social")),
                    categorySearchQuery = " WORk ",
                ).toUiModel(display()),
            )

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
            requireNotNull(
                VaultUiState(
                    selectedSort = createdAtDescendingSort(),
                    categories = VaultCategoriesState.Failed,
                ).toUiModel(display()),
            )

        assertEquals(VaultCategoriesState.Failed, model.toolbar.categoriesState)
        assertTrue(model.toolbar.matchingCategories.isEmpty())
    }

    private fun display() =
        VaultListDisplayUiModel(
            cardPresentations = emptyList(),
            swipeLeftAction = SwipeActionType.DELETE,
            swipeRightAction = SwipeActionType.COPY_PASSWORD,
            isSwipeEnabled = true,
            isPullToSearchEnabled = false,
            collapseTopBarOnScroll = false,
            collapseQuickFilterBarOnScroll = false,
            hideSystemBars = false,
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
                otpPreview = "ABC12",
            )

        val ui = item.toUiModel()

        assertEquals("entry-1", ui.id)
        assertEquals("Mail", ui.title)
        assertEquals("user@example.com", ui.username)
        assertEquals("Personal", ui.category)
        assertEquals("example.com", ui.associatedDomain)
        assertEquals("com.example", ui.associatedAppPackage)
        assertEquals(null, ui.iconName)
        assertEquals("icons/mail.png", ui.iconCustomPath)
        assertEquals(LocalIconImageModel("file://icons/mail.png"), ui.iconImageModel)
        assertEquals(EntryVisualCategory.EMAIL, ui.visualCategory)
        assertTrue(ui.favorite)
        assertTrue(ui.hasPassword)
        assertTrue(ui.hasOtp)
        assertEquals(VaultOtpKindUiModel.STEAM, ui.otpKind)
        assertEquals("ABC12", ui.otpPreview)
        assertEquals(
            VaultPasswordCardSecondaryTextUiModel.Value("example.com"),
            ui.passwordCardSecondaryText,
        )
        assertEquals(VaultPasswordCardSupportingTextUiModel.FAVORITE, ui.passwordCardSupportingText)
    }

    @Test
    fun addTypeMappingsPreserveExplicitFeatureMeaning() {
        val addTypes =
            listOf(
                AddType.PASSWORD to VaultAddTypeUiModel.PASSWORD,
                AddType.TOTP to VaultAddTypeUiModel.TOTP,
                AddType.BANK_CARD to VaultAddTypeUiModel.BANK_CARD,
                AddType.WIFI to VaultAddTypeUiModel.WIFI,
                AddType.SSH_KEY to VaultAddTypeUiModel.SSH_KEY,
                AddType.ID_CARD to VaultAddTypeUiModel.ID_CARD,
                AddType.SEED_PHRASE to VaultAddTypeUiModel.SEED_PHRASE,
                AddType.PASSKEY to VaultAddTypeUiModel.PASSKEY,
                AddType.RECOVERY_CODE to VaultAddTypeUiModel.RECOVERY_CODE,
            )

        addTypes.forEach { (feature, ui) ->
            assertEquals(ui, feature.toUiModel())
            assertEquals(feature, ui.featureType)
        }
    }
}
