package com.aozijx.passly.feature.settings.presentation.appearance

import com.aozijx.passly.domain.entry.model.query.EntryHierarchyDisplayMode
import com.aozijx.passly.domain.entry.model.query.EntrySort
import com.aozijx.passly.domain.settings.model.CardDensity
import com.aozijx.passly.domain.settings.model.EntryCardPresentation
import com.aozijx.passly.domain.settings.model.InterfaceSettings
import com.aozijx.passly.domain.settings.model.LibraryViewSettings
import com.aozijx.passly.domain.settings.model.OtpRuntimePolicy
import com.aozijx.passly.feature.settings.presentation.ui.appearance.interfaceCardStyleGroups
import org.junit.Assert.assertEquals
import org.junit.Test

class InterfaceSettingsUiMapperTest {
    @Test
    fun persistedStyleSelectsItsChoiceAndEachChoiceProducesItsDomainValue() {
        val state = map(listOf(cardPresentation("login", "password")))
        val login = interfaceCardStyleGroups(state.cardStylePresentations).single { it.entryTypeKey == "login" }

        assertEquals(listOf("default", "password"), login.options.map { it.styleKey })
        assertEquals("password", login.options.single { it.selected }.styleKey)
        assertEquals(
            listOf(
                cardPresentation("login", "default"),
                cardPresentation("login", "password"),
            ),
            login.options.map { it.selection },
        )
    }

    @Test
    fun unsupportedPersistedStyleSelectsFirstStyleSupportedByThatEntryType() {
        val state = map(listOf(cardPresentation("otp", "password")))
        val otp = interfaceCardStyleGroups(state.cardStylePresentations).single { it.entryTypeKey == "otp" }

        assertEquals(listOf("default", "totp"), otp.options.map { it.styleKey })
        assertEquals("default", otp.options.single { it.selected }.styleKey)
    }

    private fun cardPresentation(
        entryTypeKey: String,
        variantKey: String,
    ) = EntryCardPresentation(
        entryTypeKey = entryTypeKey,
        variantKey = variantKey,
        density = CardDensity.STANDARD,
        showIcon = true,
        showFavorite = true,
        showSecondaryText = true,
        showQuickAction = true,
    )

    private fun map(presentations: List<EntryCardPresentation>) =
        InterfaceSettingsUiMapper.map(
            settings =
                InterfaceSettings(
                    hideSystemBars = false,
                    collapseTopBarOnScroll = false,
                    collapseQuickFilterBarOnScroll = false,
                    appCornerRadiusDp = 16f,
                ),
            librarySettings =
                LibraryViewSettings(
                    sort = EntrySort.presets().first(),
                    entryCardPresentations = presentations,
                    entryHierarchyDisplayMode = EntryHierarchyDisplayMode.COLLAPSED,
                ),
            otpPolicy = OtpRuntimePolicy(),
        )
}
