package com.aozijx.passly.data.repository.backup

import java.util.concurrent.CancellationException
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Test

class BestEffortBackupCleanupLoggingTest {
    @Test
    fun ordinaryLogFailureDoesNotReplaceCommittedCleanupResult() {
        reportBestEffortBackupCleanupFailure {
            throw IllegalStateException("logger unavailable")
        }
    }

    @Test
    fun cancellationFromCleanupReporterStillPropagates() {
        val cancellation = CancellationException("cancelled")

        val propagated =
            assertThrows(CancellationException::class.java) {
                reportBestEffortBackupCleanupFailure { throw cancellation }
            }

        assertSame(cancellation, propagated)
    }

    @Test
    fun fatalErrorFromCleanupReporterStillPropagates() {
        val fatalError = AssertionError("fatal logger error")

        val propagated =
            assertThrows(AssertionError::class.java) {
                reportBestEffortBackupCleanupFailure { throw fatalError }
            }

        assertSame(fatalError, propagated)
    }
}
