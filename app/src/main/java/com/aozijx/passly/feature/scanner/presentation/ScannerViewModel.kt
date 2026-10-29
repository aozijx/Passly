package com.aozijx.passly.feature.scanner.presentation

import android.content.Context
import android.os.VibrationEffect
import android.os.VibratorManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aozijx.passly.domain.clipboard.port.SensitiveClipboardWriter
import com.aozijx.passly.core.observability.LogStatements
import com.aozijx.passly.core.observability.Logger
import com.aozijx.passly.core.observability.Operations
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.job
import kotlinx.coroutines.launch

@HiltViewModel
class ScannerViewModel internal constructor(
    private val clipboardWriter: SensitiveClipboardWriter,
    private val logger: Logger,
    private val decode: suspend (ImageRef) -> BarcodeImageResult,
    private val vibrate: () -> Unit,
) : ViewModel() {
    @Inject constructor(
        @ApplicationContext context: Context,
        clipboardWriter: SensitiveClipboardWriter,
        logger: Logger,
        decoder: BarcodeImageDecoder,
    ) : this(clipboardWriter, logger, decoder::decode, {
        (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager)
            .defaultVibrator.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE))
    })

    private sealed interface Work {
        class Active(val scope: CoroutineScope) : Work
        data object Stopped : Work
        data object Destroyed : Work
    }

    private data class Delivery(val owner: Work.Active, val effect: ScannerEffect)
    private val mutableState = MutableStateFlow<ScannerUiState>(ScannerUiState.Scanning)
    val uiState = mutableState.asStateFlow()
    private var work: Work = newWork()
    private val deliveries = Channel<Delivery>(Channel.CONFLATED)
    val effects = deliveries.receiveAsFlow().filter { work === it.owner }.map { it.effect }

    fun onAction(action: ScannerUiAction) {
        if (work == Work.Destroyed) return
        when (action) {
            is ScannerUiAction.BarcodeDetected -> if (uiState.value == ScannerUiState.Scanning) completeScan(action.barcode)
            is ScannerUiAction.DecodeImage -> decodeImage(action.image)
            ScannerUiAction.CopyText -> copyText()
            ScannerUiAction.StartScanning -> {
                replaceWork(newWork())
                mutate(ScannerMutation.Started)
            }
            ScannerUiAction.StopScanning -> {
                replaceWork(Work.Stopped)
                mutate(ScannerMutation.Stopped)
            }
            is ScannerUiAction.CameraFailure -> if (uiState.value == ScannerUiState.Scanning) {
                val owner = work as? Work.Active ?: return
                owner.scope.launch { reportFailure(action.error) }
            }
        }
    }

    private fun newWork() = Work.Active(CoroutineScope(
        viewModelScope.coroutineContext + SupervisorJob(viewModelScope.coroutineContext.job),
    ))

    private fun replaceWork(next: Work) {
        val previous = work
        work = next
        if (previous is Work.Active) previous.scope.cancel()
    }

    private fun completeScan(raw: String): Boolean = when (val classified = ScannerResultClassifier.classify(raw)) {
        ScannerClassification.Empty -> false
        is ScannerClassification.Classified -> {
            // A displayed result owns subsequent actions; retire pending image/copy feedback.
            replaceWork(newWork())
            vibrate()
            mutate(ScannerMutation.ScanCompleted(classified.result))
            true
        }
    }

    private fun copyText() {
        val owner = work as? Work.Active ?: return
        val shown = uiState.value as? ScannerUiState.ShowingResult ?: return
        val text = (shown.result as? ScannerResult.PlainText)?.rawValue ?: return
        owner.scope.launch {
            if (!clipboardWriter.writeSensitiveIf(text) { work === owner }) return@launch
            currentCoroutineContext().ensureActive()
            if (work === owner) deliveries.trySend(Delivery(owner, ScannerEffect.CopySucceeded))
        }
    }

    private fun decodeImage(image: ImageRef) {
        if (work !is Work.Active) return
        val owner = newWork()
        replaceWork(owner)
        mutate(ScannerMutation.ImageProcessingStarted)
        owner.scope.launch {
            val result = decode(image)
            currentCoroutineContext().ensureActive()
            if (work !== owner) return@launch
            when (result) {
                is BarcodeImageResult.Decoded -> if (!completeScan(result.rawValue)) imageUnavailable(owner, ScannerEffect.ImageNoBarcode)
                BarcodeImageResult.NoBarcode -> imageUnavailable(owner, ScannerEffect.ImageNoBarcode)
                is BarcodeImageResult.Failed -> {
                    reportFailure(result.error)
                    imageUnavailable(owner, ScannerEffect.ImageDecodeFailed)
                }
            }
        }
    }

    private fun imageUnavailable(owner: Work.Active, effect: ScannerEffect) {
        if (work !== owner) return
        mutate(ScannerMutation.ImageProcessingFailed)
        deliveries.trySend(Delivery(owner, effect))
    }

    private suspend fun reportFailure(error: Throwable) {
        logger.withRootTrace(Operations.Scanner) {
            logger.log(LogStatements.Application.scannerFailed(error))
        }
    }

    private fun mutate(mutation: ScannerMutation) {
        mutableState.value = ScannerReducer.reduce(mutableState.value, mutation)
    }

    override fun onCleared() {
        replaceWork(Work.Destroyed)
        mutate(ScannerMutation.Stopped)
        deliveries.close()
    }
}
