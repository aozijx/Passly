package com.aozijx.passly.presentation.feature.vault.list

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class VaultQuickFilterVisibilityBoundaryTest {
    @Test
    fun `active chip filters keep the quick filter surface visible`() {
        val sourceRoot = listOf(File("src/main/java"), File("app/src/main/java"))
            .firstOrNull(File::isDirectory) ?: error("Cannot locate app source root")
        val body = sourceRoot.resolve(
            "com/aozijx/passly/presentation/feature/vault/list/ui/component/list/VaultListBody.kt",
        ).readText()

        assertTrue(body.contains("val hasActiveQuickFilters ="))
        assertTrue(body.contains("hasActiveQuickFilters ||"))
    }
}
