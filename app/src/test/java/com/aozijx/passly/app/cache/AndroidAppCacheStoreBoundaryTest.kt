package com.aozijx.passly.app.cache

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AndroidAppCacheStoreBoundaryTest {
    @Test
    fun `cache store owns only Android cache directory`() {
        val sourceRoot = listOf(File("src/main/java"), File("app/src/main/java"))
            .firstOrNull(File::isDirectory) ?: error("Cannot locate app source root")
        val source = File(
            sourceRoot,
            "com/aozijx/passly/app/cache/AndroidAppCacheStore.kt",
        ).readText()

        assertTrue(source.contains("context.cacheDir"))
        assertFalse(source.contains("VaultResourcePaths"))
        assertFalse(source.contains("context.filesDir"))
    }
}
