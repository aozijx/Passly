package com.aozijx.passly.feature.scanner.presentation.state

internal sealed interface ScannerOverlayState {
    val isVisible: Boolean

    data object Hidden : ScannerOverlayState {
        override val isVisible = false
    }

    data object Visible : ScannerOverlayState {
        override val isVisible = true
    }
}
