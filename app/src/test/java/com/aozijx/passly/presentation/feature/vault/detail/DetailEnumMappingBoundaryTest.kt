package com.aozijx.passly.presentation.feature.vault.detail

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Test

class DetailEnumMappingBoundaryTest {

    @Test
    fun `detail presentation uses exhaustive mappings instead of enum names`() {
        val root = sourceRoot().resolve(
            "com/aozijx/passly/presentation/feature/vault/detail",
        )
        listOf(
            root.resolve("DetailUiMapper.kt"),
            root.resolve("DetailFaviconPresentationMapper.kt"),
        ).forEach { file ->
            val source = file.readText()
            assertFalse("${file.name} still uses valueOf", source.contains("valueOf("))
            assertFalse("${file.name} still compares enum names", source.contains(".kind.name"))
        }

        assertFalse(
            "Detail keeps a duplicate entry type UI enum",
            root.walkTopDown()
                .filter { it.isFile && it.extension == "kt" }
                .any { it.readText().contains("DetailEntryTypeUiModel") },
        )
    }

    private fun sourceRoot(): File = listOf(File("src/main/java"), File("app/src/main/java"))
        .firstOrNull(File::isDirectory) ?: error("Cannot locate app source root")
}
