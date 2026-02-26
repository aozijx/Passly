package com.example.poop.ui.screens.scanner

import android.content.Context
import android.net.Uri
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.lifecycle.ViewModel
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class ScannerViewModel : ViewModel() {
    private val _scanResult = MutableStateFlow("请将二维码置于框内")
    val scanResult: StateFlow<String> = _scanResult.asStateFlow()

    private val _hasCameraPermission = MutableStateFlow(false)
    val hasCameraPermission: StateFlow<Boolean> = _hasCameraPermission.asStateFlow()

    private val barcodeScanner = BarcodeScanning.getClient()
    val cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()

    fun onPermissionResult(isGranted: Boolean) {
        _hasCameraPermission.value = isGranted
    }

    fun onBarcodeDetected(barcode: String) {
        if (barcode.isNotBlank() && _scanResult.value != barcode) {
            _scanResult.value = barcode
        }
    }

    @ExperimentalGetImage
    fun getAnalyzer(): ImageAnalysis.Analyzer = ImageAnalysis.Analyzer { imageProxy ->
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            barcodeScanner.process(image)
                .addOnSuccessListener { barcodes ->
                    barcodes.firstOrNull()?.rawValue?.let { onBarcodeDetected(it) }
                }
                .addOnCompleteListener { imageProxy.close() }
        } else {
            imageProxy.close()
        }
    }

    fun decodeImage(context: Context, uri: Uri) {
        try {
            val image = InputImage.fromFilePath(context, uri)
            barcodeScanner.process(image)
                .addOnSuccessListener { barcodes ->
                    if (barcodes.isNotEmpty()) {
                        barcodes.first().rawValue?.let { onBarcodeDetected(it) }
                    } else {
                        _scanResult.value = "未发现二维码"
                    }
                }
                .addOnFailureListener {
                    _scanResult.value = "识别失败: ${it.message}"
                }
        } catch (e: Exception) {
            _scanResult.value = "解析图片出错"
        }
    }

    override fun onCleared() {
        super.onCleared()
        barcodeScanner.close()
        cameraExecutor.shutdown()
    }
}
