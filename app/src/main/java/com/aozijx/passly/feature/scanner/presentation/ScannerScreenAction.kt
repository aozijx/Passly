package com.aozijx.passly.feature.scanner.presentation

sealed interface ScannerScreenAction {
    data object Dismiss : ScannerScreenAction

    data object PickPhoto : ScannerScreenAction

    data object ScanAgain : ScannerScreenAction

    data object ConfirmResult : ScannerScreenAction

    data object CopyResult : ScannerScreenAction
}
