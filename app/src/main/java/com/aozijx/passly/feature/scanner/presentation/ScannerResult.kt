package com.aozijx.passly.feature.scanner.presentation

/** The scanner transports the decoded content without interpreting its domain meaning. */
data class ScannerResult(
    val rawValue: String,
)

sealed interface ScannerResultHandling {
    data object Handled : ScannerResultHandling

    data object Rejected : ScannerResultHandling
}
