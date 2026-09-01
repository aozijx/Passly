package com.aozijx.passly.app.entry.favicon

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.aozijx.passly.core.platform.VaultResourcePaths
import java.io.ByteArrayInputStream
import java.io.File
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FaviconImageStoreTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val store = FaviconImageStore(context)

    @After
    fun cleanUp() {
        VaultResourcePaths.vaultImagesDir(context).deleteRecursively()
    }

    @Test
    fun stageAndPromote_keepBytesInsideCanonicalPrivateRoots() {
        val expected = byteArrayOf(1, 2, 3)

        val staged = store.stage(ByteArrayInputStream(expected))
        val promoted = store.promote(staged)

        assertFalse(File(staged).exists())
        assertTrue(File(promoted).parentFile?.canonicalFile == VaultResourcePaths.vaultImagesDir(context).canonicalFile)
        assertTrue(File(promoted).name.startsWith("favicon_"))
        assertArrayEquals(expected, File(promoted).readBytes())
    }

    @Test
    fun stagedOperations_rejectTraversalAndCommittedFiles() {
        val outside = File(context.cacheDir, "outside.webp").apply { writeText("keep") }
        val committed = File(VaultResourcePaths.vaultImagesDir(context).apply(File::mkdirs), "favicon_keep.webp")
            .apply { writeText("keep") }

        assertThrows(FaviconImageException::class.java) { store.requireStaged(outside.absolutePath) }
        store.discard(committed.absolutePath)

        assertTrue(outside.exists())
        assertTrue(committed.exists())
        outside.delete()
    }

    @Test
    fun cleanupStaging_removesOnlyStaleStagedFiles() {
        val staged = File(store.stage(ByteArrayInputStream(byteArrayOf(1))))
        val committed = File(VaultResourcePaths.vaultImagesDir(context), "favicon_keep.webp")
            .apply { writeText("keep") }

        store.cleanupStaging()

        assertFalse(staged.exists())
        assertTrue(committed.exists())
    }
}
