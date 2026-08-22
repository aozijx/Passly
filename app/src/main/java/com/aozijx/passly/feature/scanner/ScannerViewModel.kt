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
import com.aozijx.passly.feature.scanner.contract.ScannerUiAction
import com.aozijx.passly.feature.scanner.contract.ScannerUiState
import com.aozijx.passly.feature.scanner.presentation.ScannerMutation
import com.aozijx.passly.feature.scanner.presentation.ScannerReducer
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
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

    fun onAction(action: ScannerUiAction) {
        when (action) {
            is ScannerUiAction.BarcodeDetected -> onBarcodeDetected(action.barcode)
            is ScannerUiAction.DecodeImage -> decodeImage(action.image)
            is ScannerUiAction.StartScanning -> resetAndStart()
            is ScannerUiAction.StopScanning -> stopScanning()
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
        mutate(ScannerMutation.ScanCompleted)
    }

    private fun resetAndStart() {
        lastScannedBarcode = null
        mutate(ScannerMutation.Started)
    }

    private fun stopScanning() {
        mutate(ScannerMutation.Stopped)
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
                mutate(ScannerMutation.DecodeFailed(message))
                _effects.trySend(ScannerEffect.ShowError(message))
            }
        )
    }

    private fun mutate(mutation: ScannerMutation) {
        _uiState.value = ScannerReducer.reduce(_uiState.value, mutation)
    }
}
