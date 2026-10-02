package com.aozijx.passly.presentation.feature.vault.trash

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TrashPresentationOwnershipBoundaryTest {

    @Test
    fun `trash feature owns its ui and uses explicit presentation mapping`() {
        val root = sourceRoot()
        val trashRoot = root.resolve("com/aozijx/passly/presentation/feature/vault/trash")
        val oldUiRoot = root.resolve("com/aozijx/passly/presentation/feature/vault/list/ui/trash")
        val contract = trashRoot.resolve("TrashContract.kt").readText()
        val route = trashRoot.resolve("TrashRoute.kt").readText()

        assertTrue(trashRoot.resolve("ui/TrashBottomSheet.kt").isFile)
        assertTrue(trashRoot.resolve("ui/TrashEntryCard.kt").isFile)
        assertFalse(oldUiRoot.walkTopDown().any { it.isFile && it.extension == "kt" })
        assertFalse(contract.contains("valueOf("))
        assertFalse(contract.contains("feature.vault.list"))
        assertFalse(route.contains("feature.vault.list"))
        assertTrue(contract.contains("entry.entryType.toUiModel()"))
    }

    private fun sourceRoot(): File = listOf(File("src/main/java"), File("app/src/main/java"))
        .firstOrNull(File::isDirectory) ?: error("Cannot locate app source root")
}
