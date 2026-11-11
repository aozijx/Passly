package com.aozijx.passly.feature.scanner.presentation.state

import com.aozijx.passly.feature.scanner.presentation.integration.ScannerResultPresentation
import com.aozijx.passly.feature.scanner.presentation.result.ScannerResult

internal sealed interface ScannerScreenUiState {
    val resultActions: ScannerResultActionState

    data object Scanning : ScannerScreenUiState {
        override val resultActions = ScannerResultActionState.Available
    }

    data object ProcessingImage : ScannerScreenUiState {
        override val resultActions = ScannerResultActionState.Available
    }

    data object Stopped : ScannerScreenUiState {
        override val resultActions = ScannerResultActionState.Available
    }

    data class ShowingResult(
        val result: ScannerResult,
        val presentation: ScannerResultPresentation,
        override val resultActions: ScannerResultActionState,
    ) : ScannerScreenUiState
}

internal sealed interface ScannerResultActionState {
    data object Available : ScannerResultActionState

    data object Handling : ScannerResultActionState

    data object DismissOnly : ScannerResultActionState

    data object Locked : ScannerResultActionState
}
