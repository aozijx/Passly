package com.aozijx.passly.feature.scanner.presentation

import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

internal enum class ScannerResultPhase { Ready, Handling, Handled, Closed }

internal enum class ScannerResultOutcome { Handled, Rejected, Ignored }

/** Owns confirmation only; never retains the scanned payload. Confined to the UI dispatcher. */
internal class ScannerResultSession {
    private val mutablePhase = MutableStateFlow(ScannerResultPhase.Ready)
    val phase = mutablePhase.asStateFlow()

    suspend fun confirm(handle: suspend () -> ScannerResultHandling): ScannerResultOutcome {
        if (phase.value != ScannerResultPhase.Ready) return ScannerResultOutcome.Ignored
        mutablePhase.value = ScannerResultPhase.Handling
        try {
            val result = handle()
            currentCoroutineContext().ensureActive()
            if (phase.value == ScannerResultPhase.Closed) return ScannerResultOutcome.Ignored
            return when (result) {
                ScannerResultHandling.Handled -> {
                    mutablePhase.value = ScannerResultPhase.Handled
                    ScannerResultOutcome.Handled
                }

                ScannerResultHandling.Rejected -> {
                    ScannerResultOutcome.Rejected
                }
            }
        } finally {
            if (phase.value == ScannerResultPhase.Handling) mutablePhase.value = ScannerResultPhase.Ready
        }
    }

    fun close() {
        mutablePhase.value = ScannerResultPhase.Closed
    }
}
