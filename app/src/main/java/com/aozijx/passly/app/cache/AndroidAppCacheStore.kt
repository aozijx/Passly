package com.aozijx.passly.app.cache

import android.content.Context
import com.aozijx.passly.core.platform.VaultResourcePaths
import com.aozijx.passly.core.platform.cache.DirectoryContentsCleaner
import com.aozijx.passly.core.platform.cache.DirectoryTreeSizeCalculator
import com.aozijx.passly.feature.settings.general.AppCacheStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AndroidAppCacheStore @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : AppCacheStore {
    override fun sizeBytes(): Long {
        val cacheBytes = DirectoryTreeSizeCalculator.bytes(context.cacheDir)
        val vaultImagesDir = VaultResourcePaths.vaultImagesDir(context)
        val vaultBytes = DirectoryTreeSizeCalculator.bytes(vaultImagesDir)
        return cacheBytes + vaultBytes
    }

    override fun clear() {
        DirectoryContentsCleaner.clear(context.cacheDir)
        DirectoryContentsCleaner.clear(VaultResourcePaths.vaultImagesDir(context))
    }
}