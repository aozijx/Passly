package com.aozijx.passly.feature.backup.internal.archive.platform

import kotlinx.coroutines.CancellationException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Test

class BackupSafFailureMappingTest {
    @Test
    fun `ordinary SAF operation exception becomes a failed result`() {
        val failure = SecurityException("provider denied access")

        val result = runSafOperation { throw failure }

        assertSame(failure, result.exceptionOrNull())
    }

    @Test
    fun `SAF operation cancellation propagates unchanged`() {
        val cancelled = CancellationException("provider call cancelled")

        val thrown = assertThrows(CancellationException::class.java) { runSafOperation { throw cancelled } }

        assertSame(cancelled, thrown)
    }

    @Test
    fun `SAF operation fatal error propagates unchanged`() {
        val fatal = AssertionError("fatal provider failure")

        val thrown = assertThrows(AssertionError::class.java) { runSafOperation { throw fatal } }

        assertSame(fatal, thrown)
    }

    @Test
    fun `delete maps ordinary provider exceptions to false`() {
        assertEquals(false, deleteSafDocument { throw SecurityException("delete denied") })
    }

    @Test
    fun `delete cancellation propagates unchanged`() {
        val cancelled = CancellationException("delete cancelled")

        val thrown = assertThrows(CancellationException::class.java) { deleteSafDocument { throw cancelled } }

        assertSame(cancelled, thrown)
    }

    @Test
    fun `delete fatal error propagates unchanged`() {
        val fatal = AssertionError("fatal delete failure")

        val thrown = assertThrows(AssertionError::class.java) { deleteSafDocument { throw fatal } }

        assertSame(fatal, thrown)
    }
}
