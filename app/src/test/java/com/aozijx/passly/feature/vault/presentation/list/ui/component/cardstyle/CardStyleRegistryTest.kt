package com.aozijx.passly.feature.vault.presentation.list.ui.component.cardstyle

import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.feature.vault.presentation.list.ui.model.VaultCardDensityUiModel
import com.aozijx.passly.feature.vault.presentation.list.ui.model.VaultCardPresentationUiModel
import com.aozijx.passly.feature.vault.presentation.list.model.VaultListItemUiModel
import com.aozijx.passly.presentation.shared.components.EntryVisualCategory
import com.aozijx.passly.feature.vault.presentation.list.model.VaultPasswordCardSecondaryTextUiModel
import com.aozijx.passly.feature.vault.presentation.list.model.VaultPasswordCardSupportingTextUiModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class CardStyleRegistryTest {
    @Test fun `presentation is selected by primary entry type`() {
        assertSame(
            PasswordVaultCardStyle,
            CardStyleRegistry.resolveStyle(entry(hasPassword = true), listOf(presentation("login", "password"))),
        )
    }

    @Test fun `mixed login keeps one primary card even when it also has otp`() {
        val style =
            CardStyleRegistry.resolveStyle(
                entry(hasPassword = true, hasOtp = true),
                listOf(presentation("login", "password"), presentation("otp", "totp")),
            )
        assertSame(PasswordVaultCardStyle, style)
    }

    @Test fun `login cannot select otp style even when entry has an otp field`() {
        assertSame(
            DefaultVaultCardStyle,
            CardStyleRegistry.resolveStyle(entry(hasOtp = true), listOf(presentation("login", "totp"))),
        )
    }

    @Test fun `capability mismatches and unknown keys fall back to default`() {
        assertSame(DefaultVaultCardStyle, CardStyleRegistry.resolveStyle(entry(hasPassword = true), listOf(presentation("login", "totp"))))
        assertSame(DefaultVaultCardStyle, CardStyleRegistry.resolveStyle(entry(hasOtp = true), listOf(presentation("login", "password"))))
        assertSame(
            DefaultVaultCardStyle,
            CardStyleRegistry.resolveStyle(entry(hasPassword = true), listOf(presentation("login", "unknown"))),
        )
    }

    @Test fun `all style components are discoverable through the registry`() {
        assertEquals(listOf("default", "password", "totp"), CardStyleRegistry.styles.map(VaultCardStyleComponent::key))
    }

    @Test
    fun `style preference keys are stable and independent of enum names`() {
        val expectedKeys =
            mapOf(
                EntryType.ACCOUNT to "account",
                EntryType.LOGIN to "login",
                EntryType.NOTE to "note",
                EntryType.BANK_CARD to "bank_card",
                EntryType.ID_CARD to "id_card",
                EntryType.PASSPORT to "passport",
                EntryType.DRIVER_LICENSE to "driver_license",
                EntryType.SSH_KEY to "ssh_key",
                EntryType.WIFI to "wifi",
                EntryType.PASSKEY to "passkey",
                EntryType.OTP to "otp",
                EntryType.DATABASE_CREDENTIAL to "database_credential",
                EntryType.SERVER_CREDENTIAL to "server_credential",
                EntryType.API_KEY to "api_key",
                EntryType.CRYPTO_WALLET to "crypto_wallet",
                EntryType.SEED_PHRASE to "seed_phrase",
                EntryType.RECOVERY_CODE to "recovery_code",
            )

        assertEquals(EntryType.entries.toSet(), expectedKeys.keys)
        assertEquals(expectedKeys, EntryType.entries.associateWith(CardStyleRegistry::entryTypeKey))
    }

    @Test
    fun `configurable types expose only compatible registered styles`() {
        assertEquals(
            listOf(EntryType.LOGIN, EntryType.OTP),
            CardStyleRegistry.configurableEntryTypes,
        )
        assertEquals(
            listOf("default", "password"),
            CardStyleRegistry.stylesFor(EntryType.LOGIN).map(VaultCardStyleComponent::key),
        )
        assertEquals(
            listOf("default", "totp"),
            CardStyleRegistry.stylesFor(EntryType.OTP).map(VaultCardStyleComponent::key),
        )
        assertEquals(
            listOf("default"),
            CardStyleRegistry.stylesFor(EntryType.ACCOUNT).map(VaultCardStyleComponent::key),
        )
    }

    private fun entry(
        hasPassword: Boolean = false,
        hasOtp: Boolean = false,
    ) = VaultListItemUiModel(
        id = "entry",
        entryType = EntryType.LOGIN,
        title = "Example",
        username = "user",
        category = null,
        favorite = false,
        associatedDomain = null,
        associatedAppPackage = null,
        iconName = null,
        iconCustomPath = null,
        iconImageModel = null,
        visualCategory = EntryVisualCategory.ACCOUNT,
        hasPassword = hasPassword,
        hasOtp = hasOtp,
        otpKind = null,
        otpPreview = null,
        passwordCardSecondaryText = VaultPasswordCardSecondaryTextUiModel.EntryTypeLabel(EntryType.LOGIN),
        passwordCardSupportingText = VaultPasswordCardSupportingTextUiModel.PROTECTED,
    )

    private fun presentation(
        type: String,
        variant: String,
    ) = VaultCardPresentationUiModel(
        entryTypeKey = type,
        variantKey = variant,
        density = VaultCardDensityUiModel.STANDARD,
        showIcon = true,
        showFavorite = true,
        showSecondaryText = true,
        showQuickAction = true,
    )
}
