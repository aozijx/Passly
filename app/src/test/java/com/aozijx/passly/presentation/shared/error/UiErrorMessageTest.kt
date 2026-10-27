package com.aozijx.passly.presentation.shared.error

import com.aozijx.passly.R
import com.aozijx.passly.core.error.model.DatabaseLocked
import com.aozijx.passly.core.error.model.Unexpected
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.fail
import kotlinx.coroutines.CancellationException
import org.junit.Test

class UiErrorMessageTest {
    @Test
    fun cancellationMustNotBecomeAnOrdinaryUiFailure() {
        val cancelled = CancellationException("synthetic cancellation")
        try {
            cancelled.toUiErrorMessage()
            fail("Expected cancellation to propagate")
        } catch (actual: CancellationException) {
            assertSame(cancelled, actual)
        }
    }

    @Test
    fun knownAppErrorUsesPresentationMessage() {
        assertEquals(UiErrorMessage(R.string.ui_error_database_locked), DatabaseLocked().toUiErrorMessage())
    }

    @Test
    fun unexpectedAppErrorUsesCallerFallback() {
        assertEquals(UiErrorMessage(R.string.database_session_retry_failed), Unexpected().toUiErrorMessage(R.string.database_session_retry_failed))
    }

    @Test
    fun throwableIsNormalizedBeforePresentationMapping() {
        assertEquals(UiErrorMessage(R.string.ui_error_generic), IllegalStateException("internal").toUiErrorMessage())
    }
}
