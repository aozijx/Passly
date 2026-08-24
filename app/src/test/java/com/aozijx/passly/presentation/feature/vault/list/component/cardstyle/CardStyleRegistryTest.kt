package com.aozijx.passly.presentation.feature.vault.list.component.cardstyle

import com.aozijx.passly.domain.entry.model.query.EntryCapabilities
import com.aozijx.passly.domain.entry.model.query.EntryCapability
import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.entry.model.EntryIdentity
import com.aozijx.passly.domain.entry.model.EntryProfile
import com.aozijx.passly.domain.entry.model.EntryTimestamps
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.model.query.EntryListItem
import com.aozijx.passly.domain.settings.model.EntryCardPresentation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class CardStyleRegistryTest {

    @Test
    fun `presentation is selected by primary entry type`() {
        val style = CardStyleRegistry.resolveStyle(
            entry(type = EntryType.LOGIN, capabilities = setOf(EntryCapability.PASSWORD)),
            listOf(EntryCardPresentation(entryTypeKey = "login", variantKey = "password")),
        )

        assertSame(PasswordVaultCardStyle, style)
    }

    @Test
    fun `mixed login keeps one primary card even when it also has otp`() {
        val style = CardStyleRegistry.resolveStyle(
            entry(
                type = EntryType.LOGIN,
                capabilities = setOf(EntryCapability.PASSWORD, EntryCapability.OTP),
            ),
            listOf(
                EntryCardPresentation(entryTypeKey = "login", variantKey = "password"),
                EntryCardPresentation(entryTypeKey = "otp", variantKey = "totp"),
            ),
        )

        assertSame(PasswordVaultCardStyle, style)
    }

    @Test
    fun `totp style falls back when the entry has no otp capability`() {
        val style = CardStyleRegistry.resolveStyle(
            entry(type = EntryType.LOGIN, capabilities = setOf(EntryCapability.PASSWORD)),
            listOf(EntryCardPresentation(entryTypeKey = "login", variantKey = "totp")),
        )

        assertSame(DefaultVaultCardStyle, style)
    }

    @Test
    fun `password style falls back when the entry has no password capability`() {
        val style = CardStyleRegistry.resolveStyle(
            entry(type = EntryType.LOGIN, capabilities = setOf(EntryCapability.OTP)),
            listOf(EntryCardPresentation(entryTypeKey = "login", variantKey = "password")),
        )

        assertSame(DefaultVaultCardStyle, style)
    }

    @Test
    fun `unknown style key falls back to the default component`() {
        val style = CardStyleRegistry.resolveStyle(
            entry(type = EntryType.LOGIN, capabilities = setOf(EntryCapability.PASSWORD)),
            listOf(EntryCardPresentation(entryTypeKey = "login", variantKey = "not-registered")),
        )

        assertSame(DefaultVaultCardStyle, style)
    }

    @Test
    fun `all style components are discoverable through the registry`() {
        assertEquals(
            listOf("default", "password", "totp"),
            CardStyleRegistry.styles.map(VaultCardStyleComponent::key),
        )
    }

    private fun entry(type: EntryType, capabilities: Set<EntryCapability>) = EntryListItem(
        identity = EntryIdentity(
            id = EntryId("entry"),
            type = type,
            timestamps = EntryTimestamps(1L),
        ),
        profile = EntryProfile(title = "Example", username = "user"),
        capabilities = EntryCapabilities(capabilities),
    )
}
