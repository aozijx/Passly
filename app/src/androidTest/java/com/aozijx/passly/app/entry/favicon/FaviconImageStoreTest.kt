package com.aozijx.passly.app.entry.favicon

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.aozijx.passly.app.testing.IsolatedTestContext
import com.aozijx.passly.core.platform.VaultResourcePaths
import java.io.ByteArrayInputStream
import java.io.File
import java.io.InputStream
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FaviconImageStoreTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val context: Context by lazy {
        IsolatedTestContext(ApplicationProvider.getApplicationContext(), temporaryFolder.root)
    }
    private val store by lazy { FaviconImageStore(context) }

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
    fun stageFatalReadFailure_deletesPartialInputAndPropagatesOriginalError() {
        val fatal = AssertionError("fatal input failure")
        val input =
            object : InputStream() {
                private var emitted = false

                override fun read(): Int {
                    if (!emitted) {
                        emitted = true
                        return 1
                    }
                    throw fatal
                }
            }

        val propagated = assertThrows(AssertionError::class.java) { store.stage(input) }

        assertSame(fatal, propagated)
        assertTrue(
            VaultResourcePaths
                .vaultImagesStagingDir(context)
                .listFiles()
                .orEmpty()
                .none { it.extension == "input" },
        )
    }

    @Test
    fun stagedOperations_rejectTraversalAndCommittedFiles() {
        val outside = File(context.cacheDir, "outside.webp").apply { writeText("keep") }
        val committed =
            File(VaultResourcePaths.vaultImagesDir(context).apply(File::mkdirs), "favicon_keep.webp")
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
        val committed =
            File(VaultResourcePaths.vaultImagesDir(context), "favicon_keep.webp")
                .apply { writeText("keep") }

        store.cleanupStaging()

        assertFalse(staged.exists())
        assertTrue(committed.exists())
    }

    @Test
    fun startupRecoveryIncludesBackupRestoresAndLegacyRestoredNames() {
        val root =
            VaultResourcePaths.vaultImagesDir(context).apply { mkdirs() }
        val promoted = File(root, "favicon_orphan.webp").apply { writeText("promoted") }
        val restored = File(root, "favicon_restored_orphan.webp").apply { writeText("restored") }
        val legacyRestored = File(root, "restored_legacy.webp").apply { writeText("legacy") }
        File(root, "unmanaged.txt").writeText("ignore")

        val candidates = FaviconImageStore(context).recoveredPromotedCandidates.toSet()

        assertTrue(promoted.absolutePath in candidates)
        assertTrue(restored.absolutePath in candidates)
        assertTrue(legacyRestored.absolutePath in candidates)
        assertEquals(3, candidates.size)
    }
}
