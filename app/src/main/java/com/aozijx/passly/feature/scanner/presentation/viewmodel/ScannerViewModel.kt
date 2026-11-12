package com.aozijx.passly.feature.scanner.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aozijx.passly.core.observability.LogStatements
import com.aozijx.passly.core.observability.Logger
import com.aozijx.passly.core.observability.Operations
import com.aozijx.passly.domain.clipboard.port.SensitiveClipboardWriter
import com.aozijx.passly.feature.scanner.presentation.action.ScannerUiAction
import com.aozijx.passly.feature.scanner.presentation.image.BarcodeImageDecoder
import com.aozijx.passly.feature.scanner.presentation.image.BarcodeImageResult
import com.aozijx.passly.feature.scanner.presentation.image.ImageRef
import com.aozijx.passly.feature.scanner.presentation.result.ScannerResult
import com.aozijx.passly.feature.scanner.presentation.state.ScannerFeedback
import com.aozijx.passly.feature.scanner.presentation.state.ScannerMutation
import com.aozijx.passly.feature.scanner.presentation.state.ScannerUiState
import com.aozijx.passly.feature.scanner.presentation.state.ScannerUiStateReducer
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.job
import kotlinx.coroutines.launch

@HiltViewModel
class ScannerViewModel internal constructor(
    private val clipboardWriter: SensitiveClipboardWriter,
    private val logger: Logger,
    private val decode: suspend (ImageRef) -> BarcodeImageResult,
) : ViewModel() {
    @Inject
    constructor(
        clipboardWriter: SensitiveClipboardWriter,
        logger: Logger,
        decoder: BarcodeImageDecoder,
    ) : this(clipboardWriter, logger, decoder::decode)

    private sealed interface Work {
        class Active(
            val scope: CoroutineScope,
        ) : Work

        data object HostPaused : Work

        data object Stopped : Work

        data object Destroyed : Work
    }

    private val mutableState = MutableStateFlow<ScannerUiState>(ScannerUiState.Scanning())
    internal val uiState = mutableState.asStateFlow()
    private var work: Work = newWork()
    private val nextFeedbackId = AtomicLong()

    fun onAction(action: ScannerUiAction) {
        when (work) {
            Work.Destroyed -> return

            is Work.Active,
            Work.HostPaused,
            Work.Stopped,
            -> Unit
        }
        when (action) {
            is ScannerUiAction.FeedbackConsumed -> {
                mutate(ScannerMutation.FeedbackCleared(action.id))
            }

            is ScannerUiAction.BarcodeDetected -> {
                when (uiState.value) {
                    is ScannerUiState.Scanning -> completeScan(action.barcode)

                    ScannerUiState.ProcessingImage,
                    is ScannerUiState.ShowingResult,
                    ScannerUiState.Stopped,
                    -> Unit
                }
            }

            is ScannerUiAction.DecodeImage -> {
                decodeImage(action.image)
            }

            ScannerUiAction.CopyText -> {
                copyText()
            }

            ScannerUiAction.StartScanning -> {
                replaceWork(newWork())
                mutate(ScannerMutation.Started)
            }

            ScannerUiAction.HostStarted -> {
                resumeForHost()
            }

            ScannerUiAction.HostStopped -> {
                mutate(ScannerMutation.FeedbackCleared())
                pauseForHost()
            }

            ScannerUiAction.StopScanning -> {
                replaceWork(Work.Stopped)
                mutate(ScannerMutation.Stopped)
            }

            is ScannerUiAction.CameraFailure -> {
                when (uiState.value) {
                    is ScannerUiState.Scanning -> {
                        val owner = work as? Work.Active ?: return
                        owner.scope.launch { reportFailure(action.error) }
                    }

                    ScannerUiState.ProcessingImage,
                    is ScannerUiState.ShowingResult,
                    ScannerUiState.Stopped,
                    -> {
                    }
                }
            }
        }
    }

    private fun newWork() =
        Work.Active(
            CoroutineScope(
                viewModelScope.coroutineContext + SupervisorJob(viewModelScope.coroutineContext.job),
            ),
        )

    private fun replaceWork(next: Work) {
        val previous = work
        work = next
        when (previous) {
            is Work.Active -> previous.scope.cancel()

            Work.HostPaused,
            Work.Stopped,
            Work.Destroyed,
            -> Unit
        }
    }

    private fun pauseForHost() {
        when (work) {
            Work.HostPaused, Work.Stopped, Work.Destroyed -> {
                return
            }

            is Work.Active -> {
                when (uiState.value) {
                    is ScannerUiState.ShowingResult -> {
                        replaceWork(newWork())
                    }

                    is ScannerUiState.Scanning,
                    ScannerUiState.ProcessingImage,
                    ScannerUiState.Stopped,
                    -> {
                        replaceWork(Work.HostPaused)
                        mutate(ScannerMutation.Stopped)
                    }
                }
            }
        }
    }

    private fun resumeForHost() {
        when (work) {
            is Work.Active, Work.Destroyed -> {}

            Work.HostPaused -> {
                replaceWork(newWork())
                when (uiState.value) {
                    ScannerUiState.Stopped -> mutate(ScannerMutation.Started)

                    is ScannerUiState.Scanning,
                    ScannerUiState.ProcessingImage,
                    is ScannerUiState.ShowingResult,
                    -> Unit
                }
            }

            Work.Stopped -> {}
        }
    }

    private fun completeScan(raw: String): Boolean {
        if (raw.isBlank()) return false
        // A displayed result owns subsequent actions; retire pending image/copy feedback.
        val owner = newWork()
        replaceWork(owner)
        mutate(ScannerMutation.ScanCompleted(ScannerResult(raw)))
        publishFeedback(owner, ScannerFeedback.Kind.ScanSucceeded)
        return true
    }

    private fun copyText() {
        val owner = work as? Work.Active ?: return
        val shown = uiState.value as? ScannerUiState.ShowingResult ?: return
        val text = shown.result.rawValue
        owner.scope.launch {
            if (!clipboardWriter.writeSensitiveIf(text) { work === owner }) return@launch
            currentCoroutineContext().ensureActive()
            publishFeedback(owner, ScannerFeedback.Kind.CopySucceeded)
        }
    }

    private fun decodeImage(image: ImageRef) {
        when (work) {
            Work.Stopped,
            Work.Destroyed,
            -> return

            Work.HostPaused,
            is Work.Active,
            -> Unit
        }
        val owner = newWork()
        replaceWork(owner)
        when (uiState.value) {
            ScannerUiState.Stopped -> mutate(ScannerMutation.Started)

            is ScannerUiState.Scanning,
            ScannerUiState.ProcessingImage,
            is ScannerUiState.ShowingResult,
            -> Unit
        }
        mutate(ScannerMutation.ImageProcessingStarted)
        owner.scope.launch {
            val result = decode(image)
            currentCoroutineContext().ensureActive()
            if (work !== owner) return@launch
            when (result) {
                is BarcodeImageResult.Decoded -> {
                    if (!completeScan(result.rawValue)) {
                        imageUnavailable(
                            owner,
                            ScannerFeedback.Kind.ImageNoBarcode,
                        )
                    }
                }

                BarcodeImageResult.NoBarcode -> {
                    imageUnavailable(owner, ScannerFeedback.Kind.ImageNoBarcode)
                }

                is BarcodeImageResult.Failed -> {
                    reportFailure(result.error)
                    imageUnavailable(owner, ScannerFeedback.Kind.ImageDecodeFailed)
                }
            }
        }
    }

    private fun imageUnavailable(
        owner: Work.Active,
        feedback: ScannerFeedback.Kind,
    ) {
        if (work !== owner) return
        mutate(ScannerMutation.ImageProcessingFailed)
        publishFeedback(owner, feedback)
    }

    private fun publishFeedback(
        owner: Work.Active,
        kind: ScannerFeedback.Kind,
    ) {
        if (work !== owner) return
        mutate(
            ScannerMutation.FeedbackPublished(
                ScannerFeedback(
                    nextFeedbackId.incrementAndGet(),
                    kind,
                ),
            ),
        )
    }

    private suspend fun reportFailure(error: Throwable) {
        logger.withRootTrace(Operations.Scanner) {
            logger.log(LogStatements.Application.scannerFailed(error))
        }
    }

    private fun mutate(mutation: ScannerMutation) {
        mutableState.update { ScannerUiStateReducer.reduce(it, mutation) }
    }

    override fun onCleared() {
        replaceWork(Work.Destroyed)
        mutate(ScannerMutation.Stopped)
    }
}
