package com.aozijx.passly.data.repository.backup.internal

import android.content.Context
import com.aozijx.passly.features.vault.internal.EntryIconHelper
import java.io.File
import java.io.InputStream

/**
 * 备份图片存储的内部实现。
 */
internal class BackupVInternalImageStore(context: Context) : BackupImageStore {
    private val iconHelper = EntryIconHelper()
    private val appContext = context.applicationContext

    override fun resolveReadable(path: String?): File? {
        if (path.isNullOrBlank()) return null
        val file = File(path)
        return if (file.exists() && file.canRead()) file else null
    }

    override fun saveFromBackup(fileName: String, input: InputStream): String? {
        return iconHelper.saveIconFromStream(appContext, fileName, input)
    }
}