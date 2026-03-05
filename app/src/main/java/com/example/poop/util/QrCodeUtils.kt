package com.example.poop.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import androidx.core.graphics.createBitmap
import androidx.core.graphics.set
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter

/**
 * 二维码工具类：处理生成与识别
 */
object QrCodeUtils {
    /**
     * 将字符串生成二维码 Bitmap (用于导出)
     */
    fun generateQrCode(content: String, size: Int = 512): Bitmap? {
        return try {
            val writer = QRCodeWriter()
            val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, size, size)
            val width = bitMatrix.width
            val height = bitMatrix.height
            val bitmap = createBitmap(width, height, Bitmap.Config.RGB_565)
            for (x in 0 until width) {
                for (y in 0 until height) {
                    bitmap[x, y] = if (bitMatrix.get(x, y)) Color.BLACK else Color.WHITE
                }
            }
            bitmap
        } catch (e: Exception) {
            Logcat.e("QrCodeUtils", "Generate QR code failed", e)
            null
        }
    }

    /**
     * 统一的图片二维码识别接口 (基于 ML Kit)
     */
    fun decodeFromUri(
        context: Context,
        uri: Uri,
        onSuccess: (String) -> Unit,
        onFailure: (String) -> Unit
    ) {
        try {
            val image = InputImage.fromFilePath(context, uri)
            val scanner = BarcodeScanning.getClient()
            scanner.process(image)
                .addOnSuccessListener { barcodes ->
                    val result = barcodes.firstOrNull()?.rawValue
                    if (result != null) {
                        onSuccess(result)
                    } else {
                        onFailure("未在图片中识别到二维码")
                    }
                }
                .addOnFailureListener {
                    onFailure("识别失败: ${it.message}")
                }
                .addOnCompleteListener {
                    scanner.close()
                }
        } catch (e: Exception) {
            Logcat.e("QrCodeUtils", "Error decoding image", e)
            onFailure("解析图片出错")
        }
    }
}
