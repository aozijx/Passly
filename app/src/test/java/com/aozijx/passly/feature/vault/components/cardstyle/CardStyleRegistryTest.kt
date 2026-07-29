package com.aozijx.passly.feature.vault.components.cardstyle

import com.aozijx.passly.domain.entry.model.EntryCapabilityFlags
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.model.lookup.EntryListItem
import com.aozijx.passly.domain.settings.model.EntryCardPresentation
import com.aozijx.passly.domain.settings.model.VaultCardStyle
import org.junit.Assert.assertEquals
import org.junit.Test

class CardStyleRegistryTest {

    @Test
    fun `presentation is selected by primary entry type`() {
        val style = CardStyleRegistry.resolveStyle(
            entry(type = EntryType.LOGIN, flags = EntryCapabilityFlags.HAS_PASSWORD),
            listOf(EntryCardPresentation(entryTypeKey = "login", variantKey = "password")),
        )

        assertEquals(VaultCardStyle.PASSWORD, style)
    }

    @Test
    fun `mixed login keeps one primary card even when it also has otp`() {
        val style = CardStyleRegistry.resolveStyle(
            entry(
                type = EntryType.LOGIN,
                flags = EntryCapabilityFlags.HAS_PASSWORD or EntryCapabilityFlags.HAS_OTP,
            ),
            listOf(
                EntryCardPresentation(entryTypeKey = "login", variantKey = "password"),
                EntryCardPresentation(entryTypeKey = "otp", variantKey = "totp"),
            ),
        )

        assertEquals(VaultCardStyle.PASSWORD, style)
    }

    @Test
    fun `totp style falls back when the entry has no otp capability`() {
        val style = CardStyleRegistry.resolveStyle(
            entry(type = EntryType.LOGIN, flags = EntryCapabilityFlags.HAS_PASSWORD),
            listOf(EntryCardPresentation(entryTypeKey = "login", variantKey = "totp")),
        )

        assertEquals(VaultCardStyle.DEFAULT, style)
    }

    private fun entry(type: EntryType, flags: Int) = EntryListItem(
        id = "entry",
        entryType = type,
        title = "Example",
        username = "user",
        icon = null,
        iconCustomPath = null,
        website = null,
        favorite = false,
        tags = emptyList(),
        color = null,
        createdAt = 1L,
        updatedAt = 1L,
        deletedAt = null,
        expiresAt = null,
        lastUsedAt = null,
        usageCount = 0,
        entryVersion = 1,
        capabilityFlags = flags,
    )
}
