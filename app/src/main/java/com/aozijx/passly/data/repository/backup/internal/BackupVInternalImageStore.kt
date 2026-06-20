package com.aozijx.passly.data.repository.backup.internal

import android.content.Context
import com.aozijx.passly.core.storage.VaultFileUtils
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.InputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 备份图片存储的内部实现。
 */
@Singleton
internal class BackupVInternalImageStore @Inject constructor(
    @ApplicationContext context: Context
) : BackupImageStore {
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
