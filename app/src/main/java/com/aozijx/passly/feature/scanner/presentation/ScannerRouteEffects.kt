package com.aozijx.passly.feature.scanner.presentation

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect

internal suspend fun collectScannerRouteEffects(
    lifecycle: Lifecycle,
    effects: Flow<ScannerEffect>,
    onHostStarted: () -> Unit,
    onHostStopped: () -> Unit,
    onScanSucceeded: () -> Unit,
    onUiEffect: (ScannerEffect) -> Unit,
) {
    lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
        onHostStarted()
        try {
            effects.collect { effect ->
                if (!lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) return@collect
                when (effect) {
                    ScannerEffect.ScanSucceeded -> onScanSucceeded()
                    ScannerEffect.CopySucceeded,
                    ScannerEffect.ImageNoBarcode,
                    ScannerEffect.ImageDecodeFailed,
                    -> onUiEffect(effect)
                }
            }
        } finally {
            onHostStopped()
        }
    }
}
