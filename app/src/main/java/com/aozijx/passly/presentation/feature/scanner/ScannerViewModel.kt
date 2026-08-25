package com.aozijx.passly.presentation.feature.scanner

import android.content.Context
import android.os.VibrationEffect
import android.os.VibratorManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.core.net.toUri
import com.aozijx.passly.app.clipboard.ClipboardCopyController
import com.aozijx.passly.core.otp.OtpAuthUriCodec
import com.aozijx.passly.presentation.feature.scanner.ImageRef
import com.aozijx.passly.presentation.feature.scanner.ScannerEffect
import com.aozijx.passly.presentation.feature.scanner.ScannerUiAction
import com.aozijx.passly.presentation.feature.scanner.ScannerUiState
import com.aozijx.passly.presentation.feature.scanner.ScannerMutation
import com.aozijx.passly.presentation.feature.scanner.ScannerReducer
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import javax.inject.Inject

@HiltViewModel
class ScannerViewModel @Inject constructor(
    @param:ApplicationContext private val appContext: Context,
    private val clipboardCopyController: ClipboardCopyController,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ScannerUiState())
    val uiState: StateFlow<ScannerUiState> = _uiState.asStateFlow()

    private val _effects = Channel<ScannerEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    // 防抖：缓存上次扫描结果
    private var lastScannedBarcode: String? = null

    fun copySensitive(text: String) {
        viewModelScope.launch { clipboardCopyController.copySensitive(text) }
    }

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
        val uri = image.value.toUri()
        BarcodeImageDecoder.decodeFromUri(
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
