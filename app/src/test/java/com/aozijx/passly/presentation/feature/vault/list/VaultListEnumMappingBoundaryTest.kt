package com.aozijx.passly.presentation.feature.vault.list

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Test

class VaultListEnumMappingBoundaryTest {

    @Test
    fun `vault list does not bridge enums through names`() {
        val sourceRoot = listOf(File("src/main/java"), File("app/src/main/java"))
            .firstOrNull(File::isDirectory) ?: error("Cannot locate app source root")
        val listRoot = sourceRoot.resolve(
            "com/aozijx/passly/presentation/feature/vault/list",
        )

        listOf(
            listRoot.resolve("VaultListUiMapper.kt"),
            listRoot.resolve("action/VaultSwipeActionHandler.kt"),
        ).forEach { file ->
            val source = file.readText()
            assertFalse("${file.name} compares enum names", source.contains(".direction.name"))
            assertFalse("${file.name} converts enums by name", source.contains("EntryType.valueOf"))
        }
    }
}
