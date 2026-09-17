package com.aozijx.passly.app.cache

import android.content.Context
import com.aozijx.passly.core.platform.VaultResourcePaths
import com.aozijx.passly.core.platform.cache.DirectoryContentsCleaner
import com.aozijx.passly.core.platform.cache.DirectoryTreeSizeCalculator
import com.aozijx.passly.feature.settings.general.AppCacheStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AndroidAppCacheStore @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : AppCacheStore {
    override suspend fun sizeBytes(): Long = withContext(Dispatchers.IO) {
        val cacheBytes = DirectoryTreeSizeCalculator.bytes(context.cacheDir)
        val vaultImagesDir = VaultResourcePaths.vaultImagesDir(context)
        val vaultBytes = DirectoryTreeSizeCalculator.bytes(vaultImagesDir)
        cacheBytes + vaultBytes
    }

    override suspend fun clear() = withContext(Dispatchers.IO) {
        DirectoryContentsCleaner.clear(context.cacheDir)
        DirectoryContentsCleaner.clear(VaultResourcePaths.vaultImagesDir(context))
    }
}