package com.aozijx.passly.app.cache

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.aozijx.passly.core.platform.VaultResourcePaths
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AndroidAppCacheStoreTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val store = AndroidAppCacheStore(context)
    private val cacheMarker = context.cacheDir.resolve("cache-store-test.tmp")
    private val vaultImageMarker = VaultResourcePaths.vaultImagesDir(context)
        .resolve("cache-store-persistent-test.webp")

    @Before
    fun setUp() = runBlocking {
        store.clear()
    }

    @After
    fun cleanUp() {
        cacheMarker.delete()
        vaultImageMarker.delete()
    }

    @Test
    fun sizeBytes_countsOnlyCacheDirectory() = runBlocking {
        cacheMarker.writeBytes(ByteArray(17))
        vaultImageMarker.parentFile?.mkdirs()
        vaultImageMarker.writeBytes(ByteArray(29))

        assertEquals(17L, store.sizeBytes())
    }

    @Test
    fun clear_preservesPersistentVaultImages() = runBlocking {
        cacheMarker.writeText("cache")
        vaultImageMarker.parentFile?.mkdirs()
        vaultImageMarker.writeText("persistent")

        store.clear()

        assertFalse(cacheMarker.exists())
        assertTrue(vaultImageMarker.exists())
    }
}
