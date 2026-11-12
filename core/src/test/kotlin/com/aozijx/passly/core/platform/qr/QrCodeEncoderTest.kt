package com.aozijx.passly.core.platform.qr

import com.google.zxing.BinaryBitmap
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class QrCodeEncoderTest {
    @Test
    fun `matrix round trips content and clears sensitive modules when closed`() {
        val content = "WIFI:S:Office;T:WPA;P:secret;;"
        val matrix =
            requireNotNull(
                QrCodeEncoder.encodeMatrix(content.toCharArray()),
            )

        assertTrue(matrix.width > 0)
        assertEquals(matrix.width, matrix.height)
        assertTrue(matrix.anyDarkModule())
        assertEquals(content, matrix.decode())

        matrix.close()

        assertFalse(matrix.anyDarkModule())
    }

    @Test
    fun `empty content has no matrix`() {
        assertNull(QrCodeEncoder.encodeMatrix(charArrayOf()))
    }

    private fun QrCodeMatrix.anyDarkModule(): Boolean =
        (0 until height).any { row ->
            (0 until width).any { column -> isDark(column, row) }
        }

    private fun QrCodeMatrix.decode(): String {
        val scale = 8
        val scaledWidth = width * scale
        val scaledHeight = height * scale
        val pixels =
            IntArray(scaledWidth * scaledHeight) { index ->
                val column = index % scaledWidth / scale
                val row = index / scaledWidth / scale
                if (isDark(column, row)) 0xFF000000.toInt() else 0xFFFFFFFF.toInt()
            }
        return QRCodeReader()
            .decode(
                BinaryBitmap(HybridBinarizer(RGBLuminanceSource(scaledWidth, scaledHeight, pixels))),
            ).text
    }
}
