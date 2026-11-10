package com.aozijx.passly.feature.scanner.presentation.viewmodel

import androidx.lifecycle.ViewModelStore
import com.aozijx.passly.domain.clipboard.port.SensitiveClipboardWriter
import com.aozijx.passly.feature.scanner.presentation.action.ScannerUiAction
import com.aozijx.passly.feature.scanner.presentation.image.BarcodeImageResult
import com.aozijx.passly.feature.scanner.presentation.image.ImageRef
import com.aozijx.passly.feature.scanner.presentation.result.ScannerResult
import com.aozijx.passly.feature.scanner.presentation.state.ScannerFeedback
import com.aozijx.passly.feature.scanner.presentation.state.ScannerUiState
import com.aozijx.passly.testing.MainDispatcherRule
import com.aozijx.passly.testing.testLogger
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ScannerViewModelTest {
    @get:Rule val main = MainDispatcherRule(StandardTestDispatcher())

    @Test
    fun `accepted scan stores success feedback`() =
        runTest {
            val model = model { BarcodeImageResult.NoBarcode }

            model.onAction(ScannerUiAction.BarcodeDetected("one scan"))
            model.onAction(ScannerUiAction.BarcodeDetected("duplicate frame"))
            runCurrent()

            assertEquals(ScannerFeedback.Kind.ScanSucceeded, model.uiState.value.feedback?.kind)
        }

    @Test
    fun `empty scan stores no success feedback`() =
        runTest {
            val model = model { BarcodeImageResult.NoBarcode }

            model.onAction(ScannerUiAction.BarcodeDetected(""))
            runCurrent()

            assertNull(model.uiState.value.feedback)
        }

    @Test
    fun `stopping scanner discards success feedback from a late image decode`() =
        runTest {
            val pending = CompletableDeferred<BarcodeImageResult>()
            val model = model { pending.await() }

            model.onAction(ScannerUiAction.DecodeImage(ImageRef("late")))
            runCurrent()
            model.onAction(ScannerUiAction.StopScanning)
            pending.complete(BarcodeImageResult.Decoded("too late"))
            runCurrent()

            assertEquals(ScannerUiState.Stopped, model.uiState.value)
            assertNull(model.uiState.value.feedback)
        }

    @Test
    fun `older image cannot replace a newer result even if native work ignores cancellation`() =
        runTest {
            val old = CompletableDeferred<BarcodeImageResult>()
            val model =
                model { image ->
                    if (image.value == "old") {
                        withContext(NonCancellable) { old.await() }
                    } else {
                        BarcodeImageResult.Decoded("current")
                    }
                }
            model.onAction(ScannerUiAction.DecodeImage(ImageRef("old")))
            runCurrent()
            model.onAction(ScannerUiAction.DecodeImage(ImageRef("new")))
            runCurrent()
            old.complete(BarcodeImageResult.Decoded("stale"))
            runCurrent()
            assertEquals(ScannerResult("current"), (model.uiState.value as ScannerUiState.ShowingResult).result)
        }

    @Test
    fun `rescan ignores old image failure and clears its feedback`() =
        runTest {
            val old = CompletableDeferred<BarcodeImageResult>()
            val model = model { withContext(NonCancellable) { old.await() } }
            model.onAction(ScannerUiAction.DecodeImage(ImageRef("old")))
            runCurrent()
            model.onAction(ScannerUiAction.StartScanning)
            old.complete(BarcodeImageResult.Failed(IllegalStateException("decoder failed")))
            runCurrent()
            assertEquals(ScannerUiState.Scanning(), model.uiState.value)
            assertNull(model.uiState.value.feedback)
        }

    @Test
    fun `camera cannot replace shown result or interrupt image processing`() =
        runTest {
            val pending = CompletableDeferred<BarcodeImageResult>()
            val model = model { pending.await() }
            model.onAction(ScannerUiAction.BarcodeDetected("first"))
            model.onAction(ScannerUiAction.BarcodeDetected("second"))
            assertEquals(ScannerResult("first"), (model.uiState.value as ScannerUiState.ShowingResult).result)
            model.onAction(ScannerUiAction.DecodeImage(ImageRef("image")))
            model.onAction(ScannerUiAction.BarcodeDetected("late camera"))
            assertEquals(ScannerUiState.ProcessingImage, model.uiState.value)
            pending.complete(BarcodeImageResult.Decoded("from image"))
            runCurrent()
            assertEquals(ScannerResult("from image"), (model.uiState.value as ScannerUiState.ShowingResult).result)
        }

    @Test
    fun `explicitly stopped scanner rejects late input`() =
        runTest {
            var decodes = 0
            val model =
                model {
                    decodes++
                    BarcodeImageResult.Decoded("image")
                }
            model.onAction(ScannerUiAction.StopScanning)
            model.onAction(ScannerUiAction.HostStarted)
            model.onAction(ScannerUiAction.DecodeImage(ImageRef("late")))
            model.onAction(ScannerUiAction.BarcodeDetected("late"))
            runCurrent()
            assertEquals(0, decodes)
            assertEquals(ScannerUiState.Stopped, model.uiState.value)
        }

    @Test
    fun `destroyed scanner rejects late input`() =
        runTest {
            var decodes = 0
            val model =
                model {
                    decodes++
                    BarcodeImageResult.Decoded("image")
                }
            ViewModelStore().apply {
                put("scanner", model)
                clear()
            }
            model.onAction(ScannerUiAction.DecodeImage(ImageRef("late")))
            model.onAction(ScannerUiAction.StartScanning)
            model.onAction(ScannerUiAction.BarcodeDetected("after destroy"))
            runCurrent()
            assertEquals(0, decodes)
            assertEquals(ScannerUiState.Stopped, model.uiState.value)
        }

    @Test
    fun `empty image result returns to scanning with feedback state`() =
        runTest {
            val model = model { BarcodeImageResult.NoBarcode }
            model.onAction(ScannerUiAction.DecodeImage(ImageRef("empty")))
            runCurrent()
            assertTrue(model.uiState.value is ScannerUiState.Scanning)
            assertEquals(ScannerFeedback.Kind.ImageNoBarcode, model.uiState.value.feedback?.kind)
        }

    @Test
    fun `scan replacement prevents a pending text copy from reaching clipboard`() =
        runTest {
            val clipboard = DelayedClipboard()
            val model = model(clipboardWriter = clipboard) { BarcodeImageResult.NoBarcode }
            model.onAction(ScannerUiAction.BarcodeDetected("text to copy"))
            model.onAction(ScannerUiAction.CopyText)
            runCurrent()
            clipboard.writeStarted.await()
            model.onAction(ScannerUiAction.StartScanning)
            clipboard.finishWrite.complete(Unit)
            runCurrent()
            assertNull(clipboard.lastValue)
            assertEquals(ScannerFeedback.Kind.ScanSucceeded, model.uiState.value.feedback?.kind)
        }

    @Test
    fun `accepted camera result invalidates feedback from failed image`() =
        runTest {
            val model = model { BarcodeImageResult.NoBarcode }
            model.onAction(ScannerUiAction.DecodeImage(ImageRef("empty")))
            runCurrent()
            model.onAction(ScannerUiAction.BarcodeDetected("current camera result"))
            runCurrent()
            assertEquals(ScannerFeedback.Kind.ScanSucceeded, model.uiState.value.feedback?.kind)
        }

    @Test
    fun `pending image feedback is discarded after scan restarts`() =
        runTest {
            val model = model { BarcodeImageResult.NoBarcode }
            model.onAction(ScannerUiAction.DecodeImage(ImageRef("empty")))
            runCurrent()
            model.onAction(ScannerUiAction.StartScanning)
            runCurrent()
            assertNull(model.uiState.value.feedback)
        }

    @Test
    fun `host pause preserves a visible result but restarts a stopped scan`() =
        runTest {
            val model = model { BarcodeImageResult.Decoded("selected photo") }

            model.onAction(ScannerUiAction.BarcodeDetected("visible result"))
            model.onAction(ScannerUiAction.HostStopped)
            assertEquals(ScannerResult("visible result"), (model.uiState.value as ScannerUiState.ShowingResult).result)

            model.onAction(ScannerUiAction.HostStarted)
            assertEquals(ScannerResult("visible result"), (model.uiState.value as ScannerUiState.ShowingResult).result)

            model.onAction(ScannerUiAction.StartScanning)
            model.onAction(ScannerUiAction.HostStopped)
            assertEquals(ScannerUiState.Stopped, model.uiState.value)
            model.onAction(ScannerUiAction.HostStarted)
            model.onAction(ScannerUiAction.DecodeImage(ImageRef("photo-picker-result")))
            runCurrent()

            assertEquals(ScannerResult("selected photo"), (model.uiState.value as ScannerUiState.ShowingResult).result)
        }

    @Test
    fun `photo picker result can take ownership while host remains paused`() =
        runTest {
            val model = model { BarcodeImageResult.Decoded("selected photo") }

            model.onAction(ScannerUiAction.HostStopped)
            model.onAction(ScannerUiAction.DecodeImage(ImageRef("photo-picker-result")))
            runCurrent()

            assertEquals(ScannerResult("selected photo"), (model.uiState.value as ScannerUiState.ShowingResult).result)
        }

    @Test
    fun `host pause prevents an in-flight copy from writing after foreground loss`() =
        runTest {
            val clipboard = DelayedClipboard()
            val model = model(clipboardWriter = clipboard) { BarcodeImageResult.NoBarcode }

            model.onAction(ScannerUiAction.BarcodeDetected("sensitive text"))
            model.onAction(ScannerUiAction.CopyText)
            runCurrent()
            clipboard.writeStarted.await()

            model.onAction(ScannerUiAction.HostStopped)
            clipboard.finishWrite.complete(Unit)
            runCurrent()

            assertNull(clipboard.lastValue)
            assertEquals(ScannerFeedback.Kind.ScanSucceeded, model.uiState.value.feedback?.kind)
            assertEquals(ScannerResult("sensitive text"), (model.uiState.value as ScannerUiState.ShowingResult).result)
        }

    private fun model(
        clipboardWriter: SensitiveClipboardWriter = SensitiveClipboardWriter { },
        decode: suspend (ImageRef) -> BarcodeImageResult,
    ) = ScannerViewModel(
        clipboardWriter = clipboardWriter,
        logger = testLogger(),
        decode = decode,
    )

    private class DelayedClipboard : SensitiveClipboardWriter {
        val writeStarted = CompletableDeferred<Unit>()
        val finishWrite = CompletableDeferred<Unit>()
        var lastValue: String? = null

        override suspend fun writeSensitive(text: String) {
            writeStarted.complete(Unit)
            withContext(NonCancellable) { finishWrite.await() }
            lastValue = text
        }

        override suspend fun writeSensitiveIf(
            text: String,
            canWrite: () -> Boolean,
        ): Boolean {
            writeStarted.complete(Unit)
            withContext(NonCancellable) { finishWrite.await() }
            if (!canWrite()) return false
            lastValue = text
            return true
        }
    }
}
