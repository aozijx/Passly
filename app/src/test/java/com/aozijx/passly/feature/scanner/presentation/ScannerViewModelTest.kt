package com.aozijx.passly.feature.scanner.presentation

import com.aozijx.passly.domain.clipboard.port.SensitiveClipboardWriter
import com.aozijx.passly.testing.MainDispatcherRule
import com.aozijx.passly.testing.testLogger
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ScannerViewModelTest {
    @get:Rule val main = MainDispatcherRule(StandardTestDispatcher())

    @Test
    fun `older image cannot replace a newer result even if native work ignores cancellation`() = runTest {
        val old = CompletableDeferred<BarcodeImageResult>()
        val model = model { image ->
            if (image.value == "old") withContext(NonCancellable) { old.await() }
            else BarcodeImageResult.Decoded("current")
        }
        model.onAction(ScannerUiAction.DecodeImage(ImageRef("old")))
        runCurrent()
        model.onAction(ScannerUiAction.DecodeImage(ImageRef("new")))
        runCurrent()
        old.complete(BarcodeImageResult.Decoded("stale"))
        runCurrent()
        assertEquals(ScannerUiState.ShowingResult(ScannerResult.PlainText("current")), model.uiState.value)
    }

    @Test
    fun `rescan ignores old image failure and its feedback`() = runTest {
        val old = CompletableDeferred<BarcodeImageResult>()
        val model = model { withContext(NonCancellable) { old.await() } }
        val effects = mutableListOf<ScannerEffect>()
        backgroundScope.launch(main.testDispatcher) { model.effects.toList(effects) }
        model.onAction(ScannerUiAction.DecodeImage(ImageRef("old")))
        runCurrent()
        model.onAction(ScannerUiAction.StartScanning)
        old.complete(BarcodeImageResult.Failed(IllegalStateException("decoder failed")))
        runCurrent()
        assertEquals(ScannerUiState.Scanning, model.uiState.value)
        assertEquals(emptyList<ScannerEffect>(), effects)
    }

    @Test
    fun `camera cannot replace shown result or interrupt image processing`() = runTest {
        val pending = CompletableDeferred<BarcodeImageResult>()
        val model = model { pending.await() }
        model.onAction(ScannerUiAction.BarcodeDetected("first"))
        model.onAction(ScannerUiAction.BarcodeDetected("second"))
        assertEquals(ScannerUiState.ShowingResult(ScannerResult.PlainText("first")), model.uiState.value)
        model.onAction(ScannerUiAction.DecodeImage(ImageRef("image")))
        model.onAction(ScannerUiAction.BarcodeDetected("late camera"))
        assertEquals(ScannerUiState.ProcessingImage, model.uiState.value)
        pending.complete(BarcodeImageResult.Decoded("from image"))
        runCurrent()
        assertEquals(ScannerUiState.ShowingResult(ScannerResult.PlainText("from image")), model.uiState.value)
    }

    @Test
    fun `stopped and destroyed scanner rejects late input`() = runTest {
        var decodes = 0
        val model = model { decodes++; BarcodeImageResult.Decoded("image") }
        model.onAction(ScannerUiAction.StopScanning)
        model.onAction(ScannerUiAction.DecodeImage(ImageRef("late")))
        model.onAction(ScannerUiAction.BarcodeDetected("late"))
        runCurrent()
        assertEquals(0, decodes)
        assertEquals(ScannerUiState.Stopped, model.uiState.value)
        model.javaClass.getDeclaredMethod("onCleared").apply { isAccessible = true }.invoke(model)
        model.onAction(ScannerUiAction.StartScanning)
        model.onAction(ScannerUiAction.BarcodeDetected("after destroy"))
        assertEquals(ScannerUiState.Stopped, model.uiState.value)
    }

    @Test
    fun `empty image result returns to scanning with localized feedback event`() = runTest {
        val model = model { BarcodeImageResult.NoBarcode }
        val effects = mutableListOf<ScannerEffect>()
        backgroundScope.launch(main.testDispatcher) { model.effects.toList(effects) }
        model.onAction(ScannerUiAction.DecodeImage(ImageRef("empty")))
        runCurrent()
        assertEquals(ScannerUiState.Scanning, model.uiState.value)
        assertEquals(listOf(ScannerEffect.ImageNoBarcode), effects)
    }

    @Test
    fun `scan replacement prevents a pending text copy from reaching clipboard`() = runTest {
        val clipboard = DelayedClipboard()
        val model = model(clipboardWriter = clipboard) { BarcodeImageResult.NoBarcode }
        val effects = mutableListOf<ScannerEffect>()
        backgroundScope.launch(main.testDispatcher) { model.effects.toList(effects) }
        model.onAction(ScannerUiAction.BarcodeDetected("text to copy"))
        model.onAction(ScannerUiAction.CopyText)
        runCurrent()
        clipboard.writeStarted.await()
        model.onAction(ScannerUiAction.StartScanning)
        clipboard.finishWrite.complete(Unit)
        runCurrent()
        assertNull(clipboard.lastValue)
        assertEquals(emptyList<ScannerEffect>(), effects)
    }

    @Test
    fun `accepted camera result invalidates queued feedback from failed image`() = runTest {
        val model = model { BarcodeImageResult.NoBarcode }
        model.onAction(ScannerUiAction.DecodeImage(ImageRef("empty")))
        runCurrent()
        model.onAction(ScannerUiAction.BarcodeDetected("current camera result"))
        val effects = mutableListOf<ScannerEffect>()
        backgroundScope.launch(main.testDispatcher) { model.effects.toList(effects) }
        runCurrent()
        assertEquals(emptyList<ScannerEffect>(), effects)
    }

    @Test
    fun `buffered image feedback is discarded after scan restarts`() = runTest {
        val model = model { BarcodeImageResult.NoBarcode }
        model.onAction(ScannerUiAction.DecodeImage(ImageRef("empty")))
        runCurrent()
        model.onAction(ScannerUiAction.StartScanning)
        val effects = mutableListOf<ScannerEffect>()
        backgroundScope.launch(main.testDispatcher) { model.effects.toList(effects) }
        runCurrent()
        assertEquals(emptyList<ScannerEffect>(), effects)
    }

    private fun model(
        clipboardWriter: SensitiveClipboardWriter = SensitiveClipboardWriter { },
        decode: suspend (ImageRef) -> BarcodeImageResult,
    ) = ScannerViewModel(
        clipboardWriter = clipboardWriter,
        logger = testLogger(),
        decode = decode,
        vibrate = {},
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

        override suspend fun writeSensitiveIf(text: String, canWrite: () -> Boolean): Boolean {
            writeStarted.complete(Unit)
            withContext(NonCancellable) { finishWrite.await() }
            if (!canWrite()) return false
            lastValue = text
            return true
        }
    }
}
