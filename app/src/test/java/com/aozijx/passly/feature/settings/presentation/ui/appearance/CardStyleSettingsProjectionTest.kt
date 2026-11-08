package com.aozijx.passly.feature.settings.presentation.ui.appearance

import com.aozijx.passly.domain.settings.model.EntryCardPresentation
import org.junit.Assert.assertEquals
import org.junit.Test

class CardStyleSettingsProjectionTest {
    @Test
    fun persistedStyleSelectsItsChoiceAndEachChoiceProducesItsDomainValue() {
        val groups =
            CardStyleSettingsProjection.from(
                listOf(EntryCardPresentation(entryTypeKey = "login", variantKey = "password")),
            )
        val login = groups.single { it.entryTypeKey == "login" }

        assertEquals(listOf("default", "password"), login.options.map { it.styleKey })
        assertEquals("password", login.options.single { it.selected }.styleKey)
        assertEquals(
            listOf(
                EntryCardPresentation("login", "default"),
                EntryCardPresentation("login", "password"),
            ),
            login.options.map { it.selection },
        )
    }

    @Test
    fun unsupportedPersistedStyleSelectsFirstStyleSupportedByThatEntryType() {
        val groups =
            CardStyleSettingsProjection.from(
                listOf(EntryCardPresentation(entryTypeKey = "otp", variantKey = "password")),
            )
        val otp = groups.single { it.entryTypeKey == "otp" }

        assertEquals(listOf("default", "totp"), otp.options.map { it.styleKey })
        assertEquals("default", otp.options.single { it.selected }.styleKey)
    }
}
