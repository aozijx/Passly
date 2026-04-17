package com.aozijx.passly.features.vault.internal

import android.content.Context
import android.net.Uri
import com.aozijx.passly.core.storage.VaultFileUtils
import com.aozijx.passly.domain.model.core.VaultEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

internal class EntryIconHelper {

    suspend fun saveCustomIcon(context: Context, item: VaultEntry, uri: Uri): String? {
        return withContext(Dispatchers.IO) {
            item.iconCustomPath?.let { VaultFileUtils.deleteImage(it) }
            VaultFileUtils.saveImageToInternalStorage(context, uri)
        }
    }

    /**
     * 从输入流保存图标到内部存储（通常用于备份恢复场景）。
     */
    fun saveIconFromStream(context: Context, fileName: String, input: InputStream): String? {
        return try {
            val directory = File(context.filesDir, "vault_images").apply {
                if (!exists()) mkdirs()
            }
            val destFile = File(directory, fileName)
            FileOutputStream(destFile).use { output ->
                input.copyTo(output)
            }
            destFile.absolutePath
        } catch (e: Exception) {
            null
        }
    }

    fun cleanupIcon(path: String?) {
        path?.let { VaultFileUtils.deleteImage(it) }
    }
}