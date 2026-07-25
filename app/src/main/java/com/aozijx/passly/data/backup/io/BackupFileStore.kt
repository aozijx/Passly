package com.aozijx.passly.data.backup.io

/**
 * 文件存储接口，抽象备份的 IO 层。
 * 当前实现通过 Android ContentResolver 读写 URI。
 */
interface BackupFileStore {
    fun writeBytes(uri: String, data: ByteArray)
    fun readBytesSafely(uri: String, maxBytes: Long = MAX_READ_BYTES): ByteArray
    suspend fun checkWritable(uri: String): com.aozijx.passly.core.error.AppResult<Unit>

    companion object {
        const val MAX_READ_BYTES = 256 * 1024 * 1024L // 256MB
    }
}
