package com.aozijx.passly.feature.scanner.presentation

import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

internal enum class ScannerImportPhase { Ready, Importing, Applied, Closed }
internal enum class ScannerImportOutcome { Applied, Rejected, Ignored }

/** Owns confirmation only; never retains the scanned payload. Confined to the UI dispatcher. */
internal class ScannerImportSession {
    private val mutablePhase = MutableStateFlow(ScannerImportPhase.Ready)
    val phase = mutablePhase.asStateFlow()

    suspend fun confirm(import: suspend () -> ScannerImportHandlingResult): ScannerImportOutcome {
        if (phase.value != ScannerImportPhase.Ready) return ScannerImportOutcome.Ignored
        mutablePhase.value = ScannerImportPhase.Importing
        try {
            val result = import()
            currentCoroutineContext().ensureActive()
            if (phase.value == ScannerImportPhase.Closed) return ScannerImportOutcome.Ignored
            return when (result) {
                ScannerImportHandlingResult.Applied -> {
                    mutablePhase.value = ScannerImportPhase.Applied
                    ScannerImportOutcome.Applied
                }
                ScannerImportHandlingResult.Rejected -> ScannerImportOutcome.Rejected
            }
        } finally {
            if (phase.value == ScannerImportPhase.Importing) mutablePhase.value = ScannerImportPhase.Ready
        }
    }

    fun close() {
        mutablePhase.value = ScannerImportPhase.Closed
    }
}
