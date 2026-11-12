package com.aozijx.passly.feature.scanner.presentation.route

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import com.aozijx.passly.domain.clipboard.port.SensitiveClipboardWriter
import com.aozijx.passly.feature.scanner.presentation.action.ScannerUiAction
import com.aozijx.passly.feature.scanner.presentation.image.BarcodeImageResult
import com.aozijx.passly.feature.scanner.presentation.image.ImageRef
import com.aozijx.passly.feature.scanner.presentation.result.ScannerResult
import com.aozijx.passly.feature.scanner.presentation.state.ScannerFeedback
import com.aozijx.passly.feature.scanner.presentation.state.ScannerUiState
import com.aozijx.passly.feature.scanner.presentation.viewmodel.ScannerViewModel
import com.aozijx.passly.testing.MainDispatcherRule
import com.aozijx.passly.testing.testLogger
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ScannerRouteFeedbackTest {
    @get:Rule val main = MainDispatcherRule(StandardTestDispatcher())

    @Test
    fun `route consumes pending feedback from ui state and acknowledges it`() =
        runTest {
            val pending = ScannerFeedback(1, ScannerFeedback.Kind.ScanSucceeded)
            val state = MutableStateFlow<ScannerUiState>(ScannerUiState.Scanning(pending))
            val lifecycleOwner = Owner().apply { lifecycle.currentState = Lifecycle.State.STARTED }
            var hapticCount = 0
            val uiFeedback = mutableListOf<ScannerFeedback.Kind>()
            val collector =
                launch {
                    collectScannerRouteFeedback(
                        lifecycle = lifecycleOwner.lifecycle,
                        uiState = state,
                        onHostStarted = {},
                        onHostStopped = {},
                        onFeedback = { feedback ->
                            if (feedback.kind == ScannerFeedback.Kind.ScanSucceeded) {
                                hapticCount++
                            } else {
                                uiFeedback += feedback.kind
                            }
                        },
                        onFeedbackConsumed = { id ->
                            state.update { current ->
                                val scanning = current as ScannerUiState.Scanning
                                scanning.copy(feedback = scanning.feedback?.takeUnless { it.id == id })
                            }
                        },
                    )
                }
            runCurrent()

            assertEquals(1, hapticCount)
            assertEquals(emptyList<ScannerFeedback.Kind>(), uiFeedback)
            assertEquals(null, state.value.feedback)

            state.value = ScannerUiState.Scanning(ScannerFeedback(2, ScannerFeedback.Kind.CopySucceeded))
            runCurrent()

            assertEquals(1, hapticCount)
            assertEquals(listOf(ScannerFeedback.Kind.CopySucceeded), uiFeedback)
            assertEquals(null, state.value.feedback)

            collector.cancel()
            assertEquals(1, hapticCount)
        }

    @Test
    fun `visible feedback is consumed once and host stop clears pending feedback`() =
        runTest {
            val owner = Owner().apply { lifecycle.currentState = Lifecycle.State.CREATED }
            val model = ScannerViewModel(SensitiveClipboardWriter { }, testLogger()) { BarcodeImageResult.NoBarcode }
            var hapticCount = 0
            val messages = mutableListOf<ScannerFeedback.Kind>()
            backgroundScope.launch {
                collectScannerRouteFeedback(
                    lifecycle = owner.lifecycle,
                    uiState = model.uiState,
                    onHostStarted = { model.onAction(ScannerUiAction.HostStarted) },
                    onHostStopped = { model.onAction(ScannerUiAction.HostStopped) },
                    onFeedback = { feedback ->
                        if (feedback.kind == ScannerFeedback.Kind.ScanSucceeded) hapticCount++ else messages += feedback.kind
                    },
                    onFeedbackConsumed = { model.onAction(ScannerUiAction.FeedbackConsumed(it)) },
                )
            }
            runCurrent()
            owner.lifecycle.currentState = Lifecycle.State.STARTED
            runCurrent()

            model.onAction(ScannerUiAction.BarcodeDetected("visible scan"))
            runCurrent()
            owner.lifecycle.currentState = Lifecycle.State.CREATED
            runCurrent()
            assertEquals(1, hapticCount)
            assertEquals(ScannerResult("visible scan"), (model.uiState.value as ScannerUiState.ShowingResult).result)
            assertNull(model.uiState.value.feedback)

            owner.lifecycle.currentState = Lifecycle.State.STARTED
            runCurrent()
            assertEquals(1, hapticCount)
            assertEquals(emptyList<ScannerFeedback.Kind>(), messages)

            model.onAction(ScannerUiAction.StartScanning)
            model.onAction(ScannerUiAction.BarcodeDetected("foreground scan"))
            runCurrent()
            assertEquals(2, hapticCount)
            assertEquals(emptyList<ScannerFeedback.Kind>(), messages)
        }

    @Test
    fun `host stop cancels background image work and photo picker result can resume`() =
        runTest {
            val owner = Owner().apply { lifecycle.currentState = Lifecycle.State.CREATED }
            val decodeStarted = CompletableDeferred<Unit>()
            val pendingDecode = CompletableDeferred<BarcodeImageResult>()
            val model =
                ScannerViewModel(
                    clipboardWriter = SensitiveClipboardWriter { },
                    logger = testLogger(),
                    decode = { image ->
                        if (image.value == "before-picker-stop") {
                            decodeStarted.complete(Unit)
                            withContext(NonCancellable) { pendingDecode.await() }
                        } else {
                            BarcodeImageResult.Decoded(image.value)
                        }
                    },
                )
            val feedback = mutableListOf<ScannerFeedback.Kind>()
            backgroundScope.launch {
                collectScannerRouteFeedback(
                    lifecycle = owner.lifecycle,
                    uiState = model.uiState,
                    onHostStarted = { model.onAction(ScannerUiAction.HostStarted) },
                    onHostStopped = { model.onAction(ScannerUiAction.HostStopped) },
                    onFeedback = { feedback += it.kind },
                    onFeedbackConsumed = { model.onAction(ScannerUiAction.FeedbackConsumed(it)) },
                )
            }
            runCurrent()
            owner.lifecycle.currentState = Lifecycle.State.STARTED
            runCurrent()

            model.onAction(ScannerUiAction.DecodeImage(ImageRef("before-picker-stop")))
            runCurrent()
            decodeStarted.await()
            owner.lifecycle.currentState = Lifecycle.State.CREATED
            runCurrent()
            pendingDecode.complete(BarcodeImageResult.Failed(IllegalStateException("stale decode")))
            runCurrent()

            assertEquals(ScannerUiState.Stopped, model.uiState.value)
            assertEquals(emptyList<ScannerFeedback.Kind>(), feedback)

            owner.lifecycle.currentState = Lifecycle.State.STARTED
            runCurrent()
            model.onAction(ScannerUiAction.DecodeImage(ImageRef("photo-picker-result")))
            runCurrent()
            assertEquals(ScannerResult("photo-picker-result"), (model.uiState.value as ScannerUiState.ShowingResult).result)
            assertEquals(listOf(ScannerFeedback.Kind.ScanSucceeded), feedback)
        }

    private class Owner : LifecycleOwner {
        override val lifecycle = LifecycleRegistry.createUnsafe(this)
    }
}
