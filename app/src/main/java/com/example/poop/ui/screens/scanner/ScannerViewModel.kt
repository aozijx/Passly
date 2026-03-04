package com.example.poop.ui.screens.scanner

import android.content.Context
import android.net.Uri
import android.os.VibrationEffect
import android.os.VibratorManager
import androidx.lifecycle.ViewModel
import com.example.poop.util.Logcat
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ScannerViewModel : ViewModel() {
    private val _scanResult = MutableStateFlow("")
    val scanResult: StateFlow<String> = _scanResult.asStateFlow()

    private val barcodeScanner = BarcodeScanning.getClient()

    /**
     * 扫码或识别成功后的统一处理回调
     */
    fun onBarcodeDetected(context: Context, barcode: String) {
        if (barcode.isNotBlank() && _scanResult.value != barcode) {
            _scanResult.value = barcode
            vibrate(context)
        }
    }

    private fun vibrate(context: Context) {
        val vibratorManager =
            context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        val vibrator = vibratorManager.defaultVibrator
        vibrator.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE))
    }

    /**
     * 识别相册选取的图片
     */
    fun decodeImage(context: Context, uri: Uri) {
        try {
            val image = InputImage.fromFilePath(context, uri)
            barcodeScanner.process(image)
                .addOnSuccessListener { barcodes ->
                    if (barcodes.isNotEmpty()) {
                        barcodes.firstOrNull()?.rawValue?.let { onBarcodeDetected(context, it) }
                    } else {
                        _scanResult.value = "未发现二维码"
                    }
                }
                .addOnFailureListener {
                    _scanResult.value = "识别失败"
                }
        } catch (e: Exception) {
            Logcat.e("ScannerViewModel", "Error decoding image", e)
            _scanResult.value = "解析图片出错"
        }
    }

    override fun onCleared() {
        super.onCleared()
        barcodeScanner.close()
    }
}
