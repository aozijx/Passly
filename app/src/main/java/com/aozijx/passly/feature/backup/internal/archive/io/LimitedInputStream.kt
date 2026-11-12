package com.aozijx.passly.feature.backup.internal.archive.io

import java.io.FilterInputStream
import java.io.InputStream

/**
 * 限制读取最大字节数的 InputStream 包装器。
 * 防止内存溢出（OOM）。
 */
internal class LimitedInputStream(
    input: InputStream,
    private val maxBytes: Long,
) : FilterInputStream(input) {
    private var totalRead: Long = 0

    init {
        require(maxBytes >= 0)
    }

    override fun read(): Int {
        val result = super.read()
        if (result >= 0) {
            totalRead++
            check(totalRead <= maxBytes) { "输入流超过大小限制: $maxBytes 字节" }
        }
        return result
    }

    override fun read(
        b: ByteArray,
        off: Int,
        len: Int,
    ): Int {
        require(off >= 0 && len >= 0 && off <= b.size - len)
        if (len == 0) return 0
        val remaining = maxBytes - totalRead
        val allowed = if (remaining >= len.toLong()) len else (remaining + 1).toInt()
        val result = super.read(b, off, allowed)
        if (result > 0) {
            totalRead += result
            check(totalRead <= maxBytes) { "输入流超过大小限制: $maxBytes 字节" }
        }
        return result
    }
}
