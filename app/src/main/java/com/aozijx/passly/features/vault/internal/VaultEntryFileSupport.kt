package com.aozijx.passly.features.vault.internal

import android.content.Context
import android.net.Uri
import com.aozijx.passly.core.storage.VaultFileUtils
import com.aozijx.passly.domain.model.core.VaultEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal class VaultEntryFileSupport {

    suspend fun saveCustomIcon(context: Context, item: VaultEntry, uri: Uri): String? {
        return withContext(Dispatchers.IO) {
            item.iconCustomPath?.let { VaultFileUtils.deleteImage(it) }
            VaultFileUtils.saveImageToInternalStorage(context, uri)
        }
    }

    fun cleanupIcon(path: String?) {
        path?.let { VaultFileUtils.deleteImage(it) }
    }
}
