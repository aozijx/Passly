package com.aozijx.passly.core.storage

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.UUID

object VaultFileUtils {

    suspend fun saveImageToInternalStorage(context: Context, uri: Uri): String? {
        return withContext(Dispatchers.IO) {
            try {
                val directory = File(context.filesDir, "vault_images").apply {
                    if (!exists()) mkdirs()
                }

                val fileName = "img_${UUID.randomUUID()}.jpg"
                val destFile = File(directory, fileName)

                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    FileOutputStream(destFile).use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }

                destFile.absolutePath
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    suspend fun saveImageFromStream(context: Context, fileName: String, input: InputStream): String? {
        return withContext(Dispatchers.IO) {
            try {
                val directory = File(context.filesDir, "vault_images").apply {
                    if (!exists()) mkdirs()
                }
                val destFile = File(directory, fileName)
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
                destFile.absolutePath
            } catch (_: Exception) {
                null
            }
        }
    }

    suspend fun deleteImage(path: String?): Boolean {
        if (path.isNullOrEmpty()) return false
        return withContext(Dispatchers.IO) {
            try {
                val file = File(path)
                file.delete()
            } catch (_: Exception) {
                false
            }
        }
    }
}



