package com.aozijx.passly.feature.scanner.presentation.action

import com.aozijx.passly.feature.scanner.presentation.state.ScannerOverlayState
import com.aozijx.passly.feature.scanner.presentation.state.ScannerResultActionState

internal fun resolveScannerScreenAction(
    action: ScannerScreenAction,
    overlay: ScannerOverlayState,
    resultActions: ScannerResultActionState,
): ScannerScreenAction? =
    when (overlay) {
        ScannerOverlayState.Hidden -> null
        ScannerOverlayState.Visible ->
            when (resultActions) {
                ScannerResultActionState.Available -> action
                ScannerResultActionState.Handling,
                ScannerResultActionState.DismissOnly,
                -> when (action) {
                    ScannerScreenAction.Dismiss -> action
                    ScannerScreenAction.PickPhoto,
                    ScannerScreenAction.ScanAgain,
                    ScannerScreenAction.CopyResult,
                    ScannerScreenAction.ConfirmResult,
                    -> null
                }

                ScannerResultActionState.Locked -> null
            }
    }
