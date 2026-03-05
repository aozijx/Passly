package com.example.poop.ui.screens.scanner

import android.content.Context
import android.net.Uri
import android.os.VibrationEffect
import android.os.VibratorManager
import androidx.lifecycle.ViewModel
import com.example.poop.util.QrCodeUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ScannerViewModel : ViewModel() {
    private val _scanResult = MutableStateFlow("")
    val scanResult: StateFlow<String> = _scanResult.asStateFlow()

    /**
     * 扫码或识别成功后的统一处理回调
     */
    fun onBarcodeDetected(context: Context, barcode: String) {
        if (barcode.isNotBlank() && _scanResult.value != barcode) {
            _scanResult.value = barcode
            vibrate(context)
        } else if (barcode.isBlank()) {
            _scanResult.value = ""
        }
    }

    private fun vibrate(context: Context) {
        val vibratorManager =
            context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        val vibrator = vibratorManager.defaultVibrator
        vibrator.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE))
    }

    /**
     * 识别相册选取的图片，复用 QrCodeUtils 逻辑
     */
    fun decodeImage(context: Context, uri: Uri) {
        QrCodeUtils.decodeFromUri(
            context = context,
            uri = uri,
            onSuccess = { result -> onBarcodeDetected(context, result) },
            onFailure = { message -> _scanResult.value = message }
        )
    }
}
