package com.aozijx.passly.core.storage

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.UUID

object VaultFileUtils {

    /**
     * 将 URI 指向的图片拷贝到 App 内部存储中，实现持久化持有。
     * @return 拷贝后的本地绝对路径，如果失败则返回 null
     */
    fun saveImageToInternalStorage(context: Context, uri: Uri): String? {
        return try {
            // 创建 vault_images 文件夹
            val directory = File(context.filesDir, "vault_images").apply {
                if (!exists()) mkdirs()
            }
            
            // 生成唯一文件名，防止冲突
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

    /**
     * 从输入流保存图标到内部存储（通常用于备份恢复场景）。
     */
    fun saveImageFromStream(context: Context, fileName: String, input: InputStream): String? {
        return try {
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

    /**
     * 删除不再需要的本地图片文件
     */
    fun deleteImage(path: String?): Boolean {
        if (path.isNullOrEmpty()) return false
        return try {
            val file = File(path)
            if (file.exists()) file.delete() else false
        } catch (_: Exception) {
            false
        }
    }
}



