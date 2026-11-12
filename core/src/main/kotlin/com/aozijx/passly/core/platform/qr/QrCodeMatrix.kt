package com.aozijx.passly.core.platform.qr

/** Owns the sensitive QR modules used by a single display session. */
class QrCodeMatrix internal constructor(
    val width: Int,
    val height: Int,
    private val modules: LongArray,
) : AutoCloseable {
    private var closed = false

    init {
        require(width > 0 && height > 0)
        require(modules.size == (width * height + Long.SIZE_BITS - 1) / Long.SIZE_BITS)
    }

    fun isDark(
        column: Int,
        row: Int,
    ): Boolean {
        require(column in 0 until width)
        require(row in 0 until height)
        if (closed) return false
        val moduleIndex = row * width + column
        return modules[moduleIndex / Long.SIZE_BITS] and (1L shl (moduleIndex % Long.SIZE_BITS)) != 0L
    }

    override fun close() {
        if (closed) return
        modules.fill(0L)
        closed = true
    }

    override fun toString() = "QrCodeMatrix(width=$width, height=$height)"
}
