package com.aozijx.passly.core.platform.qr

import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.QRCodeWriter

object QrCodeEncoder {
    fun encodeMatrix(content: CharArray): QrCodeMatrix? =
        encodeBitMatrix(content)?.let { matrix ->
            try {
                val moduleCount = matrix.width * matrix.height
                val modules = LongArray((moduleCount + Long.SIZE_BITS - 1) / Long.SIZE_BITS)
                for (row in 0 until matrix.height) {
                    for (column in 0 until matrix.width) {
                        if (matrix[column, row]) {
                            val moduleIndex = row * matrix.width + column
                            modules[moduleIndex / Long.SIZE_BITS] =
                                modules[moduleIndex / Long.SIZE_BITS] or
                                (1L shl (moduleIndex % Long.SIZE_BITS))
                        }
                    }
                }
                QrCodeMatrix(matrix.width, matrix.height, modules)
            } finally {
                matrix.clear()
            }
        }

    private fun encodeBitMatrix(content: CharArray): BitMatrix? {
        if (content.isEmpty()) return null
        return try {
            QRCodeWriter().encode(
                String(content),
                BarcodeFormat.QR_CODE,
                0,
                0,
                mapOf(EncodeHintType.CHARACTER_SET to "UTF-8"),
            )
        } catch (_: Exception) {
            null
        }
    }
}
