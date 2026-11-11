package com.aozijx.passly.feature.scanner.presentation.integration

internal enum class ScannerPrimaryActionLabel {
    Continue,
    Save,
    Use,
}

internal sealed interface ScannerResultTextInteraction {
    data object CopyOnTap : ScannerResultTextInteraction

    data object ReadOnly : ScannerResultTextInteraction
}

internal data class ScannerResultPresentation(
    val resultTextInteraction: ScannerResultTextInteraction = ScannerResultTextInteraction.CopyOnTap,
    val primaryActionLabel: ScannerPrimaryActionLabel = ScannerPrimaryActionLabel.Continue,
) {
    companion object {
        val DEFAULT = ScannerResultPresentation()
    }
}
