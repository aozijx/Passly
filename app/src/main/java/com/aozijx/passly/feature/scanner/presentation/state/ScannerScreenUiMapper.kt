package com.aozijx.passly.feature.scanner.presentation.state

import com.aozijx.passly.feature.scanner.presentation.integration.ScannerResultPresentation
import com.aozijx.passly.feature.scanner.presentation.result.ScannerResultPhase

internal fun mapScannerScreenState(
    scanner: ScannerUiState,
    resultActions: ScannerResultActionState,
    presentation: ScannerResultPresentation,
): ScannerScreenUiState =
    when (scanner) {
        is ScannerUiState.Scanning -> {
            ScannerScreenUiState.Scanning
        }

        ScannerUiState.ProcessingImage -> {
            ScannerScreenUiState.ProcessingImage
        }

        ScannerUiState.Stopped -> {
            ScannerScreenUiState.Stopped
        }

        is ScannerUiState.ShowingResult -> {
            ScannerScreenUiState.ShowingResult(
                result = scanner.result,
                presentation = presentation,
                resultActions = resultActions,
            )
        }
    }

internal fun mapScannerResultActionState(phase: ScannerResultPhase): ScannerResultActionState =
    when (phase) {
        ScannerResultPhase.Ready -> ScannerResultActionState.Available
        ScannerResultPhase.Handling -> ScannerResultActionState.Handling
        ScannerResultPhase.Handled -> ScannerResultActionState.DismissOnly
        ScannerResultPhase.Closed -> ScannerResultActionState.Locked
    }
