package com.aozijx.passly.feature.backup.internal.archive.io

import java.io.FilterOutputStream
import java.io.OutputStream

/** Rejects the first over-budget write before any of that write reaches the destination. */
class LimitedOutputStream(
    output: OutputStream,
    private val maxBytes: Long,
) : FilterOutputStream(output) {
    private var totalWritten = 0L

    init {
        require(maxBytes >= 0)
    }

    override fun write(value: Int) {
        check(totalWritten < maxBytes) { "输出流超过大小限制: $maxBytes 字节" }
        out.write(value)
        totalWritten++
    }

    override fun write(
        buffer: ByteArray,
        offset: Int,
        length: Int,
    ) {
        require(offset >= 0 && length >= 0 && offset <= buffer.size - length)
        check(length.toLong() <= maxBytes - totalWritten) { "输出流超过大小限制: $maxBytes 字节" }
        out.write(buffer, offset, length)
        totalWritten += length
    }
}
