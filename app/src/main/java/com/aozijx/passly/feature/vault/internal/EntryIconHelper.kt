package com.aozijx.passly.feature.vault.internal

import android.content.Context
import android.net.Uri
import com.aozijx.passly.core.storage.VaultFileUtils
import com.aozijx.passly.domain.model.entry.VaultEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal class EntryIconHelper {

    suspend fun saveCustomIcon(context: Context, item: VaultEntry, uri: Uri): String? {
        return withContext(Dispatchers.IO) {
            item.iconCustomPath?.let { VaultFileUtils.deleteImage(it) }
            VaultFileUtils.saveImageToInternalStorage(context, uri)
        }
    }

    suspend fun cleanupIcon(path: String?) {
        path?.let { VaultFileUtils.deleteImage(it) }
    }
}