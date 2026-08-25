package com.aozijx.passly.app.cache

import android.content.Context
import com.aozijx.passly.core.platform.VaultResourcePaths
import com.aozijx.passly.core.platform.cache.ByteSizeFormatter
import com.aozijx.passly.core.platform.cache.DirectoryContentsCleaner
import com.aozijx.passly.core.platform.cache.DirectoryTreeSizeCalculator
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppCacheManager @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    fun calculateTotalSize(): String {
        val cacheBytes = DirectoryTreeSizeCalculator.bytes(context.cacheDir)
        val vaultImagesDir = VaultResourcePaths.vaultImagesDir(context)
        val vaultBytes = DirectoryTreeSizeCalculator.bytes(vaultImagesDir)
        return ByteSizeFormatter.format(cacheBytes + vaultBytes)
    }

    fun clearAll() {
        DirectoryContentsCleaner.clear(context.cacheDir)
        DirectoryContentsCleaner.clear(VaultResourcePaths.vaultImagesDir(context))
    }
}
