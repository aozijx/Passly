package com.aozijx.passly.presentation.shared.components

import org.junit.Assert.assertEquals
import org.junit.Test

class VaultIconPaletteTest {
    @Test
    fun storageTokensRoundTripAndUnknownValuesUseAutomaticColor() {
        val expected =
            mapOf(
                VaultIconColorToken.PRIMARY to "primary",
                VaultIconColorToken.SECONDARY to "secondary",
                VaultIconColorToken.TERTIARY to "tertiary",
                VaultIconColorToken.ERROR to "error",
                VaultIconColorToken.NEUTRAL to "neutral",
            )

        assertEquals(VaultIconColorToken.entries.toSet(), expected.keys)
        assertEquals(expected.size, expected.values.toSet().size)
        expected.forEach { (token, storageValue) ->
            assertEquals(storageValue, token.storageValue)
            assertEquals(token, VaultIconColorToken.fromStorage(storageValue))
        }
        assertEquals(null, VaultIconColorToken.fromStorage(null))
        assertEquals(null, VaultIconColorToken.fromStorage("unknown"))
    }
}
