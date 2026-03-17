package com.example.poop.utils

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
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
