package com.example.poop.ui.screens.scanner

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class ScannerViewModel : ViewModel() {
    private val _scanResult = MutableStateFlow("请将二维码置于框内")
    val scanResult: StateFlow<String> = _scanResult.asStateFlow()

    private val _hasCameraPermission = MutableStateFlow(false)
    val hasCameraPermission: StateFlow<Boolean> = _hasCameraPermission.asStateFlow()

    fun onPermissionResult(isGranted: Boolean) {
        _hasCameraPermission.value = isGranted
    }

    fun onBarcodeDetected(barcode: String) {
        if (barcode.isNotBlank() && _scanResult.value != barcode) {
            _scanResult.value = barcode
            // 这里可以添加进一步的处理逻辑，比如解析 URL 或自动跳转
        }
    }
}
