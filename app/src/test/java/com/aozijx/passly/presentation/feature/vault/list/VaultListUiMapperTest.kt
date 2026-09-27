package com.aozijx.passly.presentation.feature.vault.list

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
import com.aozijx.passly.feature.vault.model.AddType
import com.aozijx.passly.presentation.feature.vault.list.ui.model.VaultAddTypeUiModel
import com.aozijx.passly.presentation.feature.vault.list.ui.model.VaultListDisplayUiModel
import com.aozijx.passly.presentation.feature.vault.list.ui.model.VaultOtpKindUiModel
import com.aozijx.passly.presentation.shared.entry.EntryTypeUiModel
import com.aozijx.passly.presentation.shared.gesture.SwipeActionUiModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VaultListUiMapperTest {
    @Test
    fun dialogChangesAreIsolatedFromPagingAndPageChromeState() {
        val display = VaultListDisplayUiModel(
            cardPresentations = emptyList(),
            swipeLeftAction = SwipeActionUiModel.DELETE,
            swipeRightAction = SwipeActionUiModel.DETAIL,
            isSwipeEnabled = true,
            collapseTopBarOnScroll = false,
            collapseQuickFilterBarOnScroll = false,
            hideSystemBars = false,
        )
        val initial = VaultUiState().toUiModel(display)
        val withDialog = VaultUiState(addType = AddType.BANK_CARD)
            .toUiModel(display)

        assertEquals(initial.toolbar, withDialog.toolbar)
        assertEquals(initial.navigation, withDialog.navigation)
        assertEquals(initial.content, withDialog.content)
        assertEquals(initial.layout, withDialog.layout)
        assertEquals(VaultAddTypeUiModel.BANK_CARD, withDialog.dialogs.addType)
    }

    @Test
    fun entryMappingPreservesEveryRenderedValue() {
        val item = EntryListItem(
            identity = EntryIdentity(
                id = EntryId("entry-1"),
                type = EntryType.LOGIN,
                timestamps = EntryTimestamps(1L),
            ),
            profile = EntryProfile(
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
        assertTrue(ui.favorite)
        assertTrue(ui.hasPassword)
        assertTrue(ui.hasOtp)
        assertEquals(VaultOtpKindUiModel.STEAM, ui.otpKind)
        assertEquals("ABC12", ui.otpPreview)
    }

    @Test
    fun enumMappingsPreserveExplicitFeatureMeaning() {
        val entryTypes = listOf(
            EntryType.ACCOUNT to EntryTypeUiModel.ACCOUNT,
            EntryType.LOGIN to EntryTypeUiModel.LOGIN,
            EntryType.NOTE to EntryTypeUiModel.NOTE,
            EntryType.BANK_CARD to EntryTypeUiModel.BANK_CARD,
            EntryType.ID_CARD to EntryTypeUiModel.ID_CARD,
            EntryType.PASSPORT to EntryTypeUiModel.PASSPORT,
            EntryType.DRIVER_LICENSE to EntryTypeUiModel.DRIVER_LICENSE,
            EntryType.SSH_KEY to EntryTypeUiModel.SSH_KEY,
            EntryType.WIFI to EntryTypeUiModel.WIFI,
            EntryType.PASSKEY to EntryTypeUiModel.PASSKEY,
            EntryType.OTP to EntryTypeUiModel.OTP,
            EntryType.DATABASE_CREDENTIAL to EntryTypeUiModel.DATABASE_CREDENTIAL,
            EntryType.SERVER_CREDENTIAL to EntryTypeUiModel.SERVER_CREDENTIAL,
            EntryType.API_KEY to EntryTypeUiModel.API_KEY,
            EntryType.CRYPTO_WALLET to EntryTypeUiModel.CRYPTO_WALLET,
            EntryType.SEED_PHRASE to EntryTypeUiModel.SEED_PHRASE,
            EntryType.RECOVERY_CODE to EntryTypeUiModel.RECOVERY_CODE,
        )
        val addTypes = listOf(
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

        entryTypes.forEach { (domain, ui) -> assertEquals(ui, domain.toUiModel()) }
        addTypes.forEach { (feature, ui) ->
            assertEquals(ui, feature.toUiModel())
            assertEquals(feature, ui.toFeatureModel())
        }
    }
}
