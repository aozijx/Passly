package com.aozijx.passly.feature.scanner.presentation.state

import com.aozijx.passly.feature.scanner.presentation.result.ScannerResult

internal sealed interface ScannerUiState {
    val feedback: ScannerFeedback?
        get() = null

    data class Scanning(
        override val feedback: ScannerFeedback? = null,
    ) : ScannerUiState

    data object ProcessingImage : ScannerUiState

    data class ShowingResult(
        val result: ScannerResult,
        override val feedback: ScannerFeedback? = null,
    ) : ScannerUiState

    data object Stopped : ScannerUiState
}

internal data class ScannerFeedback(
    val id: Long,
    val kind: Kind,
) {
    enum class Kind {
        ScanSucceeded,
        CopySucceeded,
        ImageNoBarcode,
        ImageDecodeFailed,
    }
}
