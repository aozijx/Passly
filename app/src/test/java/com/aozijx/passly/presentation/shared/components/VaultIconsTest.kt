package com.aozijx.passly.presentation.shared.components

import com.aozijx.passly.presentation.shared.entryvisual.EntryVisualCategory
import com.aozijx.passly.presentation.shared.entryvisual.VaultItemAssociatedAppUiState
import com.aozijx.passly.presentation.shared.entryvisual.VaultItemIconGlyphUiState
import com.aozijx.passly.presentation.shared.entryvisual.VaultItemIconImageUiState
import com.aozijx.passly.presentation.shared.entryvisual.VaultItemIconTintUiState
import com.aozijx.passly.presentation.shared.entryvisual.vaultItemIconUiModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VaultIconsTest {
    @Test
    fun catalogUsesUniqueStableKeysAndNonemptyCategories() {
        val definitions = VaultIcons.definitions

        assertTrue(definitions.isNotEmpty())
        assertEquals(definitions.size, definitions.map { it.key }.toSet().size)
        assertTrue(definitions.all { it.key.isNotBlank() })
        assertTrue(definitions.all { it.searchAliases.isNotEmpty() })
    }

    @Test
    fun stableKeyResolvesItsDefinition() {
        VaultIcons.definitions.forEach { definition ->
            assertEquals(definition, VaultIcons.findDefinition(definition.key))
            assertEquals(definition.imageVector, VaultIcons.findDefinition(definition.key)?.imageVector)
        }
    }

    @Test
    fun decimalResourceIdStringIsNotAStoredIconKey() {
        val resourceId =
            VaultIcons.definitions
                .first()
                .labelRes
                .toString()

        assertEquals(null, VaultIcons.findDefinition(resourceId))
    }

    @Test
    fun searchMatchesAliasIgnoringCaseAndCanFilterCategory() {
        val allMatches = VaultIcons.search("BANK")
        val financeMatches = VaultIcons.search("bank", VaultIconCategory.FINANCE)

        assertTrue(allMatches.any { it.key == "finance.bank" })
        assertEquals(listOf("finance.bank"), financeMatches.map { it.key })
    }

    @Test
    fun unknownKeyIsNotAStoredDefinition() {
        assertEquals(null, VaultIcons.findDefinition("missing.icon"))
    }

    @Test
    fun iconProjectionResolvesStableKeysAndFallsBackForSearchAliases() {
        val named =
            vaultItemIconUiModel(
                iconName = "communication.email",
                customImagePath = null,
                iconColor = "blue",
                associatedAppPackage = " ",
                visualCategory = EntryVisualCategory.EMAIL,
            )
        val alias =
            vaultItemIconUiModel(
                iconName = "mail",
                customImagePath = null,
                associatedAppPackage = null,
                visualCategory = EntryVisualCategory.EMAIL,
            )

        assertEquals(VaultItemIconImageUiState.Default, named.image)
        assertTrue(named.glyph is VaultItemIconGlyphUiState.Named)
        assertEquals(
            VaultItemIconTintUiState.StorageToken("blue"),
            (named.glyph as VaultItemIconGlyphUiState.Named).tint,
        )
        assertEquals(VaultItemAssociatedAppUiState.None, named.associatedApp)
        assertEquals(VaultItemIconGlyphUiState.CategoryFallback, alias.glyph)
    }
}
