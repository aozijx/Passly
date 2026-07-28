package com.aozijx.passly.feature.scanner

import android.content.Context
import android.net.Uri
import android.os.VibrationEffect
import android.os.VibratorManager
import androidx.lifecycle.ViewModel
import com.aozijx.passly.core.qr.QrCodeUtils
import com.aozijx.passly.core.util.TotpUtils
import com.aozijx.passly.feature.scanner.contract.ScannerEffect
import com.aozijx.passly.feature.scanner.contract.ScannerIntent
import com.aozijx.passly.feature.scanner.contract.ScannerUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class ScannerViewModel @Inject constructor(
    @param:ApplicationContext private val appContext: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(ScannerUiState())
    val uiState: StateFlow<ScannerUiState> = _uiState.asStateFlow()

    private val _effects = MutableSharedFlow<ScannerEffect>(extraBufferCapacity = 1)
    val effects: SharedFlow<ScannerEffect> = _effects.asSharedFlow()

    // 防抖：缓存上次扫描结果
    private var lastScannedBarcode: String? = null

    fun handleIntent(intent: ScannerIntent) {
        when (intent) {
            is ScannerIntent.BarcodeDetected -> onBarcodeDetected(intent.barcode)
            is ScannerIntent.DecodeImage -> decodeImage(intent.uri)
            is ScannerIntent.StartScanning -> resetAndStart()
            is ScannerIntent.StopScanning -> stopScanning()
        }
    }

    private fun onBarcodeDetected(barcode: String) {
        if (barcode.isBlank() || barcode == lastScannedBarcode) return
        lastScannedBarcode = barcode
        vibrate()
        _effects.tryEmit(
            ScannerEffect.ScanSuccess(
                result = barcode,
                otpConfig = TotpUtils.parseOtpAuthUri(barcode)
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

    private fun decodeImage(uri: Uri) {
        QrCodeUtils.decodeFromUri(
            context = appContext,
            uri = uri,
            onSuccess = { onBarcodeDetected(it) },
            onFailure = { message ->
                _uiState.update { it.copy(error = message) }
                _effects.tryEmit(ScannerEffect.ShowError(message))
            }
        )
    }
}
