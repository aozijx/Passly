package com.aozijx.passly.data.repository.backup.internal

import java.io.File
import java.io.InputStream

/**
 * 备份图片存储契约。
 * 仅限数据层内部使用。
 */
internal interface BackupImageStore {
    fun resolveReadable(path: String?): File?
    fun saveFromBackup(fileName: String, input: InputStream): String?
}