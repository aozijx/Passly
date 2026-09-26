package com.aozijx.passly.presentation.feature.vault.list

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VaultListEventOwnershipBoundaryTest {
    @Test
    fun `vault list uses typed event sinks without handler bridges`() {
        val sourceRoot = sourceRoot()
        val listRoot = sourceRoot.resolve(
            "com/aozijx/passly/presentation/feature/vault/list",
        )
        val route = listRoot.resolve("VaultRoute.kt").readText()
        val screen = listRoot.resolve("ui/VaultScreen.kt").readText()
        val models = listRoot.resolve("ui/model/VaultListUiModels.kt").readText()

        assertFalse(listRoot.resolve("VaultListEventRouting.kt").readText().contains(
            "rememberVaultListEventHandler",
        ))
        assertFalse(listRoot.resolve("VaultListItemEventHandler.kt").exists())
        assertFalse(models.contains("VaultListEventHandler"))
        assertFalse(models.contains("VaultListItemEventHandler"))
        assertTrue(models.contains("sealed interface VaultListItemEvent"))
        assertTrue(screen.contains("onEvent: (VaultListEvent) -> Unit"))
        assertTrue(screen.contains("onItemEvent: (VaultListItemEvent) -> Unit"))
        assertFalse(route.contains("rememberVaultListEventHandler"))
        assertFalse(route.contains("rememberVaultListItemEventHandler"))
    }

    private fun sourceRoot(): File = listOf(File("src/main/java"), File("app/src/main/java"))
        .firstOrNull(File::isDirectory) ?: error("Cannot locate app source root")
}
