package com.aozijx.passly.feature.scanner.presentation

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect

internal suspend fun collectScannerRouteEffects(
    effects: Flow<ScannerEffect>,
    onScanSucceeded: () -> Unit,
    onUiEffect: (ScannerEffect) -> Unit,
) {
    effects.collect { effect ->
        when (effect) {
            ScannerEffect.ScanSucceeded -> onScanSucceeded()
            ScannerEffect.CopySucceeded,
            ScannerEffect.ImageNoBarcode,
            ScannerEffect.ImageDecodeFailed,
            -> onUiEffect(effect)
        }
    }
}
