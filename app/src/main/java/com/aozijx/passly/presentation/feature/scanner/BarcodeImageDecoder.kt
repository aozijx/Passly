package com.aozijx.passly.presentation.feature.scanner

import android.content.Context
import android.net.Uri
import com.aozijx.passly.app.diagnostics.AppTelemetry
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage

/**
 * 使用 ML Kit 从用户选择的图片中识别条码。
 */
object BarcodeImageDecoder {
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
            AppTelemetry.e("BarcodeImageDecoder", "Error decoding image", e)
            onFailure("解析图片出错")
        }
    }
}
