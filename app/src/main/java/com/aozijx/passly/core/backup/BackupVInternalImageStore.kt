package com.aozijx.passly.core.backup

import android.content.Context
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

class BackupVInternalImageStore(context: Context) : BackupImageStore {
    private val appContext = context.applicationContext

    override fun resolveReadable(path: String?): File? {
        if (path.isNullOrBlank()) return null
        val file = File(path)
        return file.takeIf { it.exists() && it.isFile }
    }

    override fun saveFromBackup(fileName: String, input: InputStream): String? {
        return try {
            val directory = File(appContext.filesDir, "vault_images").apply {
                if (!exists()) mkdirs()
            }
            val safeName = fileName.replace("..", "_")
            val target = File(directory, safeName)
            FileOutputStream(target).use { output ->
                input.copyTo(output)
            }
            target.absolutePath
        } catch (_: Exception) {
            null
        }
    }
}

