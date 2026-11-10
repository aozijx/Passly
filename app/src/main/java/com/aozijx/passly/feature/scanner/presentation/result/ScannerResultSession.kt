package com.aozijx.passly.feature.scanner.presentation.result

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

internal sealed interface ScannerResultPhase {
    data object Ready : ScannerResultPhase

    data object Handling : ScannerResultPhase

    data object Handled : ScannerResultPhase

    data object Closed : ScannerResultPhase
}

internal sealed interface ScannerResultOutcome {
    data object Handled : ScannerResultOutcome

    data object Rejected : ScannerResultOutcome

    data object Ignored : ScannerResultOutcome

    data class Failed(
        val error: Exception,
    ) : ScannerResultOutcome
}

/** Owns confirmation only; never retains the scanned payload. Confined to the UI dispatcher. */
internal class ScannerResultSession {
    private val mutablePhase = MutableStateFlow<ScannerResultPhase>(ScannerResultPhase.Ready)
    val phase = mutablePhase.asStateFlow()

    suspend fun confirm(handle: suspend () -> ScannerResultHandling): ScannerResultOutcome {
        when (phase.value) {
            ScannerResultPhase.Ready -> Unit

            ScannerResultPhase.Handling,
            ScannerResultPhase.Handled,
            ScannerResultPhase.Closed,
            -> return ScannerResultOutcome.Ignored
        }
        mutablePhase.value = ScannerResultPhase.Handling
        try {
            val result = handle()
            currentCoroutineContext().ensureActive()
            when (phase.value) {
                ScannerResultPhase.Closed -> return ScannerResultOutcome.Ignored

                ScannerResultPhase.Ready,
                ScannerResultPhase.Handling,
                ScannerResultPhase.Handled,
                -> Unit
            }
            return when (result) {
                ScannerResultHandling.Handled -> {
                    mutablePhase.value = ScannerResultPhase.Handled
                    ScannerResultOutcome.Handled
                }

                ScannerResultHandling.Rejected -> {
                    ScannerResultOutcome.Rejected
                }

                is ScannerResultHandling.Failed -> {
                    ScannerResultOutcome.Failed(result.error)
                }
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            return when (phase.value) {
                ScannerResultPhase.Closed -> ScannerResultOutcome.Ignored

                ScannerResultPhase.Ready,
                ScannerResultPhase.Handling,
                ScannerResultPhase.Handled,
                -> ScannerResultOutcome.Failed(error)
            }
        } finally {
            when (phase.value) {
                ScannerResultPhase.Handling -> mutablePhase.value = ScannerResultPhase.Ready

                ScannerResultPhase.Ready,
                ScannerResultPhase.Handled,
                ScannerResultPhase.Closed,
                -> Unit
            }
        }
    }

    fun close() {
        mutablePhase.value = ScannerResultPhase.Closed
    }
}
