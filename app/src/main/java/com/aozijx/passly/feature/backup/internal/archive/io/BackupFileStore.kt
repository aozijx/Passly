package com.aozijx.passly.feature.backup.internal.archive.io

import java.io.InputStream
import java.io.OutputStream

/**
 * 文件存储接口，抽象备份的 IO 层。
 * 当前实现通过 Android ContentResolver 读写 URI。
 */
internal interface BackupFileStore {
    fun openOutput(
        uri: String,
        maxBytes: Long = MAX_BYTES,
    ): OutputStream

    fun openInput(
        uri: String,
        maxBytes: Long = MAX_BYTES,
    ): InputStream

    companion object {
        const val MAX_BYTES = 256 * 1024 * 1024L
    }
}
