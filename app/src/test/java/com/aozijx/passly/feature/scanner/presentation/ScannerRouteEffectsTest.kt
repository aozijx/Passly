package com.aozijx.passly.feature.scanner.presentation

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ScannerRouteEffectsTest {
    @Test
    fun `route performs one success haptic and stops handling effects when its collector is cancelled`() = runTest {
        val effects = Channel<ScannerEffect>(Channel.UNLIMITED)
        var hapticCount = 0
        val uiEffects = mutableListOf<ScannerEffect>()
        val collector = launch {
            collectScannerRouteEffects(
                effects = effects.receiveAsFlow(),
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
}
