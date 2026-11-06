package com.aozijx.passly.feature.scanner.presentation

internal enum class ScannerPrimaryActionLabel {
    Continue,
    Save,
    Use,
}

internal data class ScannerResultPresentation(
    val showCopyAction: Boolean = true,
    val primaryActionLabel: ScannerPrimaryActionLabel = ScannerPrimaryActionLabel.Continue,
) {
    companion object {
        val DEFAULT = ScannerResultPresentation()
    }
}
