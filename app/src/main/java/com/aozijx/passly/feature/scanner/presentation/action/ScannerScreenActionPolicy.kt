package com.aozijx.passly.feature.scanner.presentation.action

import com.aozijx.passly.feature.scanner.presentation.state.ScannerResultActionState

internal fun resolveScannerScreenAction(
    action: ScannerScreenAction,
    resultActions: ScannerResultActionState,
): ScannerScreenAction? =
    when (resultActions) {
        ScannerResultActionState.Available -> {
            action
        }

        ScannerResultActionState.Handling,
        ScannerResultActionState.DismissOnly,
        -> {
            when (action) {
                ScannerScreenAction.Dismiss -> action

                ScannerScreenAction.PickPhoto,
                ScannerScreenAction.ScanAgain,
                ScannerScreenAction.CopyResult,
                ScannerScreenAction.ConfirmResult,
                -> null
            }
        }

        ScannerResultActionState.Locked -> {
            null
        }
    }
