package com.aozijx.passly.presentation.ui.shared.components

import org.junit.Assert.assertEquals
import org.junit.Test

class VaultIconPaletteTest {

    @Test
    fun storageTokensRoundTripAndUnknownValuesUseAutomaticColor() {
        val tokens = listOf("primary", "secondary", "tertiary", "error", "neutral")

        assertEquals(tokens, tokens.map { VaultIconColorToken.fromStorage(it)?.storageValue })
        assertEquals(null, VaultIconColorToken.fromStorage(null))
        assertEquals(null, VaultIconColorToken.fromStorage("unknown"))
    }
}
