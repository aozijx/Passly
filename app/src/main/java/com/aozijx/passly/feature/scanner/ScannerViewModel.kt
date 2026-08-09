package com.aozijx.passly.feature.scanner

import android.content.Context
import android.net.Uri
import android.os.VibrationEffect
import android.os.VibratorManager
import androidx.lifecycle.ViewModel
import com.aozijx.passly.core.otp.OtpAuthUriCodec
import com.aozijx.passly.core.util.QrCodeUtils
import com.aozijx.passly.feature.scanner.contract.ImageRef
import com.aozijx.passly.feature.scanner.contract.ScannerEffect
import com.aozijx.passly.feature.scanner.contract.ScannerIntent
import com.aozijx.passly.feature.scanner.contract.ScannerUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class ScannerViewModel @Inject constructor(
    @param:ApplicationContext private val appContext: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(ScannerUiState())
    val uiState: StateFlow<ScannerUiState> = _uiState.asStateFlow()

    private val _effects = Channel<ScannerEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    // 防抖：缓存上次扫描结果
    private var lastScannedBarcode: String? = null

    fun handleIntent(intent: ScannerIntent) {
        when (intent) {
            is ScannerIntent.BarcodeDetected -> onBarcodeDetected(intent.barcode)
            is ScannerIntent.DecodeImage -> decodeImage(intent.image)
            is ScannerIntent.StartScanning -> resetAndStart()
            is ScannerIntent.StopScanning -> stopScanning()
        }
    }

    private fun onBarcodeDetected(barcode: String) {
        if (barcode.isBlank() || barcode == lastScannedBarcode) return
        lastScannedBarcode = barcode
        vibrate()
        _effects.trySend(
            ScannerEffect.ScanSuccess(
                result = barcode,
                otpConfig = OtpAuthUriCodec.parse(barcode)
            )
        )
        _uiState.update { it.copy(isScanning = false) }
    }

    private fun resetAndStart() {
        lastScannedBarcode = null
        _uiState.value = ScannerUiState()
    }

    private fun stopScanning() {
        _uiState.update { it.copy(isScanning = false) }
    }

    private fun vibrate() {
        (appContext.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager)
            .defaultVibrator
            .vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE))
    }

    private fun decodeImage(image: ImageRef) {
        val uri = Uri.parse(image.value)
        QrCodeUtils.decodeFromUri(
            context = appContext,
            uri = uri,
            onSuccess = { onBarcodeDetected(it) },
            onFailure = { message ->
                _uiState.update { it.copy(error = message) }
                _effects.trySend(ScannerEffect.ShowError(message))
            }
        )
    }
}
