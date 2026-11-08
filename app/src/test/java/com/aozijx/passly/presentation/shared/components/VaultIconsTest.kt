package com.aozijx.passly.presentation.shared.components

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
}
