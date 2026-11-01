package com.aozijx.passly.feature.scanner.presentation

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import com.aozijx.passly.domain.clipboard.port.SensitiveClipboardWriter
import com.aozijx.passly.testing.MainDispatcherRule
import com.aozijx.passly.testing.testLogger
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ScannerRouteEffectsTest {
    @get:Rule val main = MainDispatcherRule(StandardTestDispatcher())

    @Test
    fun `route performs one success haptic and stops handling effects when its collector is cancelled`() = runTest {
        val effects = Channel<ScannerEffect>(Channel.UNLIMITED)
        val lifecycleOwner = Owner().apply { lifecycle.currentState = Lifecycle.State.STARTED }
        var hapticCount = 0
        val uiEffects = mutableListOf<ScannerEffect>()
        val collector = launch {
            collectScannerRouteEffects(
                lifecycle = lifecycleOwner.lifecycle,
                effects = effects.receiveAsFlow(),
                onHostStarted = {},
                onHostStopped = {},
                onScanSucceeded = { hapticCount++ },
                onUiEffect = uiEffects::add,
            )
        }
        runCurrent()

        effects.send(ScannerEffect.ScanSucceeded)
        effects.send(ScannerEffect.CopySucceeded)
        runCurrent()
        assertEquals(1, hapticCount)
        assertEquals(listOf(ScannerEffect.CopySucceeded), uiEffects)

        collector.cancel()
        effects.send(ScannerEffect.ScanSucceeded)
        runCurrent()
        assertEquals(1, hapticCount)
    }

    @Test
    fun `background completion feedback is dropped and a resumed scan reports once`() = runTest {
        val owner = Owner().apply { lifecycle.currentState = Lifecycle.State.CREATED }
        val model = ScannerViewModel(SensitiveClipboardWriter { }, testLogger()) { BarcodeImageResult.NoBarcode }
        var hapticCount = 0
        val messages = mutableListOf<ScannerEffect>()
        backgroundScope.launch {
            collectScannerRouteEffects(
                lifecycle = owner.lifecycle,
                effects = model.effects,
                onHostStarted = { model.onAction(ScannerUiAction.HostStarted) },
                onHostStopped = { model.onAction(ScannerUiAction.HostStopped) },
                onScanSucceeded = { hapticCount++ },
                onUiEffect = messages::add,
            )
        }
        runCurrent()
        owner.lifecycle.currentState = Lifecycle.State.STARTED
        runCurrent()

        model.onAction(ScannerUiAction.BarcodeDetected("completed before stop"))
        owner.lifecycle.currentState = Lifecycle.State.CREATED
        runCurrent()
        assertEquals(0, hapticCount)
        assertEquals(ScannerUiState.ShowingResult(ScannerResult.PlainText("completed before stop")), model.uiState.value)

        owner.lifecycle.currentState = Lifecycle.State.STARTED
        runCurrent()
        assertEquals(0, hapticCount)
        assertEquals(emptyList<ScannerEffect>(), messages)

        model.onAction(ScannerUiAction.StartScanning)
        model.onAction(ScannerUiAction.BarcodeDetected("foreground scan"))
        runCurrent()
        assertEquals(1, hapticCount)
        assertEquals(emptyList<ScannerEffect>(), messages)
    }

    @Test
    fun `host stop cancels background image work and photo picker result can resume`() = runTest {
        val owner = Owner().apply { lifecycle.currentState = Lifecycle.State.CREATED }
        val decodeStarted = CompletableDeferred<Unit>()
        val pendingDecode = CompletableDeferred<BarcodeImageResult>()
        val model = ScannerViewModel(
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
        val effects = mutableListOf<ScannerEffect>()
        backgroundScope.launch {
            collectScannerRouteEffects(
                lifecycle = owner.lifecycle,
                effects = model.effects,
                onHostStarted = { model.onAction(ScannerUiAction.HostStarted) },
                onHostStopped = { model.onAction(ScannerUiAction.HostStopped) },
                onScanSucceeded = {},
                onUiEffect = effects::add,
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
        assertEquals(emptyList<ScannerEffect>(), effects)

        owner.lifecycle.currentState = Lifecycle.State.STARTED
        runCurrent()
        model.onAction(ScannerUiAction.DecodeImage(ImageRef("photo-picker-result")))
        runCurrent()
        assertEquals(ScannerUiState.ShowingResult(ScannerResult.PlainText("photo-picker-result")), model.uiState.value)
        assertEquals(emptyList<ScannerEffect>(), effects)
    }

    private class Owner : LifecycleOwner {
        override val lifecycle = LifecycleRegistry.createUnsafe(this)
    }
}
