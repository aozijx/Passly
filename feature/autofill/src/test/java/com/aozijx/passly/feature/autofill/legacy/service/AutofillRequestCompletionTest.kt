package com.aozijx.passly.feature.autofill.legacy.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class AutofillRequestCompletionTest {
    @Test
    fun `cancellation sends one empty success and ignores a late result`() {
        val callbacks = mutableListOf<String?>()
        val completion =
            AutofillRequestCompletion<String>(
                onSuccess = callbacks::add,
                onFailure = { throw AssertionError("cancelled request must not become a failure") },
            )

        assertTrue(completion.cancel())
        assertFalse(completion.success("late result"))

        assertEquals(listOf(null), callbacks)
    }

    @Test
    fun `failed callback does not reopen completion`() {
        val callbackFailure = IllegalStateException("callback failed")
        val completion =
            AutofillRequestCompletion<String>(
                onSuccess = { throw AssertionError("success callback must not run") },
                onFailure = { throw callbackFailure },
            )

        assertThrows(IllegalStateException::class.java) {
            completion.failure("request failed")
        }

        assertFalse(completion.success("must not retry callback"))
    }
}
