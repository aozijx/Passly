package com.aozijx.passly.presentation.ui.shared.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
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
            assertEquals(definition.imageVector, VaultIcons.getIconByName(definition.key))
        }
    }

    @Test
    fun legacyResourceIdStringResolvesTheSameDefinition() {
        VaultIcons.definitions.forEach { definition ->
            definition.legacyResourceIds.forEach { resourceId ->
                assertEquals(
                    definition.imageVector,
                    VaultIcons.getIconByName(resourceId.toString()),
                )
            }
        }
    }

    @Test
    fun searchMatchesAliasIgnoringCaseAndCanFilterCategory() {
        val allMatches = VaultIcons.search("BANK")
        val financeMatches = VaultIcons.search("bank", VaultIconCategory.FINANCE)

        assertTrue(allMatches.any { it.key == "finance.bank" })
        assertEquals(listOf("finance.bank"), financeMatches.map { it.key })
    }

    @Test
    fun unknownKeyFallsBackWithoutBecomingAStoredDefinition() {
        assertEquals(null, VaultIcons.findDefinition("missing.icon"))
        assertNotNull(VaultIcons.getIconByName("missing.icon"))
    }
}
