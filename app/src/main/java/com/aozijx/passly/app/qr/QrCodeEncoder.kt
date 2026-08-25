package com.aozijx.passly.app.qr

import android.graphics.Bitmap
import android.graphics.Color
import androidx.core.graphics.createBitmap
import androidx.core.graphics.set
import com.aozijx.passly.app.diagnostics.AppTelemetry
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter

object QrCodeEncoder {
    fun encode(content: String, size: Int = 512): Bitmap? = try {
        val matrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, size, size)
        createBitmap(matrix.width, matrix.height, Bitmap.Config.RGB_565).apply {
            for (x in 0 until matrix.width) {
                for (y in 0 until matrix.height) {
                    this[x, y] = if (matrix[x, y]) Color.BLACK else Color.WHITE
                }
            }
        }
    } catch (error: Exception) {
        AppTelemetry.e("QrCodeEncoder", "Generate QR code failed", error)
        null
    }
}
