package com.aozijx.passly.feature.scanner.presentation.integration

internal data class ScannerResultPlan(
    val presentation: ScannerResultPresentation,
    val confirm: suspend () -> ScannerResultHandling,
)
