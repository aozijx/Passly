package com.aozijx.passly.data.repository.backup.internal

import android.content.Context
import com.aozijx.passly.core.storage.VaultFileUtils
import java.io.File
import java.io.InputStream

/**
 * 备份图片存储的内部实现。
 */
internal class BackupVInternalImageStore(context: Context) : BackupImageStore {
    private val appContext = context.applicationContext

    override fun resolveReadable(path: String?): File? {
        if (path.isNullOrBlank()) return null
        val file = File(path)
        return if (file.exists() && file.canRead()) file else null
    }

    override fun saveFromBackup(fileName: String, input: InputStream): String? {
        return VaultFileUtils.saveImageFromStream(appContext, fileName, input)
    }
}
