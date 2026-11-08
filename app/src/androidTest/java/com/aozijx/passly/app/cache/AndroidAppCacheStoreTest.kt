package com.aozijx.passly.app.cache

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.aozijx.passly.app.testing.IsolatedTestContext
import com.aozijx.passly.core.platform.VaultResourcePaths
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AndroidAppCacheStoreTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val context: Context by lazy {
        IsolatedTestContext(ApplicationProvider.getApplicationContext(), temporaryFolder.root)
    }
    private val store by lazy { AndroidAppCacheStore(context) }
    private val cacheMarker by lazy { context.cacheDir.resolve("cache-store-test.tmp") }
    private val vaultImageMarker by lazy {
        VaultResourcePaths
            .vaultImagesDir(context)
            .resolve("cache-store-persistent-test.webp")
    }

    @Test
    fun sizeBytes_countsOnlyCacheDirectory() =
        runBlocking {
            cacheMarker.writeBytes(ByteArray(17))
            vaultImageMarker.parentFile?.mkdirs()
            vaultImageMarker.writeBytes(ByteArray(29))

            assertEquals(17L, store.sizeBytes())
        }

    @Test
    fun clear_preservesPersistentVaultImages() =
        runBlocking {
            cacheMarker.writeText("cache")
            vaultImageMarker.parentFile?.mkdirs()
            vaultImageMarker.writeText("persistent")

            store.clear()

            assertFalse(cacheMarker.exists())
            assertTrue(vaultImageMarker.exists())
        }
}
