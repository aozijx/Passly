package com.aozijx.passly.presentation.ui.vault.list.component.cardstyle

import com.aozijx.passly.presentation.ui.vault.list.model.VaultCardDensityUiModel
import com.aozijx.passly.presentation.ui.vault.list.model.VaultCardPresentationUiModel
import com.aozijx.passly.presentation.ui.shared.entry.EntryTypeUiModel
import com.aozijx.passly.presentation.ui.vault.list.model.VaultListItemUiModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class CardStyleRegistryTest {
    @Test fun `presentation is selected by primary entry type`() {
        assertSame(PasswordVaultCardStyle, CardStyleRegistry.resolveStyle(entry(hasPassword = true), listOf(presentation("login", "password"))))
    }

    @Test fun `mixed login keeps one primary card even when it also has otp`() {
        val style = CardStyleRegistry.resolveStyle(entry(hasPassword = true, hasOtp = true), listOf(presentation("login", "password"), presentation("otp", "totp")))
        assertSame(PasswordVaultCardStyle, style)
    }

    @Test fun `capability mismatches and unknown keys fall back to default`() {
        assertSame(DefaultVaultCardStyle, CardStyleRegistry.resolveStyle(entry(hasPassword = true), listOf(presentation("login", "totp"))))
        assertSame(DefaultVaultCardStyle, CardStyleRegistry.resolveStyle(entry(hasOtp = true), listOf(presentation("login", "password"))))
        assertSame(DefaultVaultCardStyle, CardStyleRegistry.resolveStyle(entry(hasPassword = true), listOf(presentation("login", "unknown"))))
    }

    @Test fun `all style components are discoverable through the registry`() {
        assertEquals(listOf("default", "password", "totp"), CardStyleRegistry.styles.map(VaultCardStyleComponent::key))
    }

    private fun entry(hasPassword: Boolean = false, hasOtp: Boolean = false) = VaultListItemUiModel(
        id = "entry", entryType = EntryTypeUiModel.LOGIN, title = "Example", username = "user",
        category = null, favorite = false, associatedDomain = null, associatedAppPackage = null,
        iconName = null, iconCustomPath = null, hasPassword = hasPassword, hasOtp = hasOtp, otpKind = null, otpPreview = null,
    )

    private fun presentation(type: String, variant: String) = VaultCardPresentationUiModel(
        entryTypeKey = type, variantKey = variant, density = VaultCardDensityUiModel.STANDARD,
        showIcon = true, showFavorite = true, showSecondaryText = true, showQuickAction = true,
    )
}
