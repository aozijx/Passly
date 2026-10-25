package com.aozijx.passly.feature.scanner.presentation

import android.content.Context
import android.os.VibrationEffect
import android.os.VibratorManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.core.net.toUri
import com.aozijx.passly.domain.clipboard.port.SensitiveClipboardWriter
import com.aozijx.passly.core.observability.LogStatements
import com.aozijx.passly.core.observability.Logger
import com.aozijx.passly.core.observability.Operations
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
    private val clipboardWriter: SensitiveClipboardWriter,
    private val logger: Logger,
) : ViewModel() {

    private val _uiState = MutableStateFlow<ScannerUiState>(ScannerUiState.Scanning)
    val uiState: StateFlow<ScannerUiState> = _uiState.asStateFlow()

    private val _effects = Channel<ScannerEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    private val scanSession = ScannerScanSession()

    fun onAction(action: ScannerUiAction) {
        when (action) {
            is ScannerUiAction.BarcodeDetected -> onBarcodeDetected(action.barcode)
            is ScannerUiAction.DecodeImage -> decodeImage(action.image)
            ScannerUiAction.CopyText -> copyText()
            is ScannerUiAction.StartScanning -> resetAndStart()
            is ScannerUiAction.StopScanning -> stopScanning()
            is ScannerUiAction.CameraFailure -> reportFailure(action.error)
        }
    }

    private fun onBarcodeDetected(barcode: String) {
        if (!scanSession.accept(barcode)) return
        when (val classification = ScannerResultClassifier.classify(barcode)) {
            ScannerClassification.Empty -> Unit
            is ScannerClassification.Classified -> {
                vibrate()
                mutate(ScannerMutation.ScanCompleted(classification.result))
            }
        }
    }

    private fun resetAndStart() {
        scanSession.reset()
        mutate(ScannerMutation.Started)
    }

    private fun stopScanning() {
        scanSession.reset()
        mutate(ScannerMutation.Stopped)
    }

    private fun copyText() {
        val shownResult = _uiState.value as? ScannerUiState.ShowingResult ?: return
        val text = (shownResult.result as? ScannerResult.PlainText)?.rawValue ?: return
        viewModelScope.launch {
            clipboardWriter.writeSensitive(text)
            _effects.send(ScannerEffect.CopySucceeded)
        }
    }

    private fun vibrate() {
        (appContext.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager)
            .defaultVibrator
            .vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE))
    }

    private fun decodeImage(image: ImageRef) {
        mutate(ScannerMutation.ImageProcessingStarted)
        val uri = image.value.toUri()
        BarcodeImageDecoder.decodeFromUri(
            context = appContext,
            uri = uri,
            onSuccess = { onBarcodeDetected(it) },
            onFailure = { message, error ->
                error?.let(::reportFailure)
                mutate(ScannerMutation.ImageProcessingFailed)
                _effects.trySend(ScannerEffect.ShowError(message))
            }
        )
    }

    private fun reportFailure(error: Throwable) {
        viewModelScope.launch {
            logger.withRootTrace(Operations.Scanner) {
                logger.log(LogStatements.Application.scannerFailed(error))
            }
        }
    }

    private fun mutate(mutation: ScannerMutation) {
        _uiState.value = ScannerReducer.reduce(_uiState.value, mutation)
    }
}
