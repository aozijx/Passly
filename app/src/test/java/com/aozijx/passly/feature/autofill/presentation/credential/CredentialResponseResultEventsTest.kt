package com.aozijx.passly.feature.autofill.presentation.credential

import android.content.Intent
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class CredentialResponseResultEventsTest {
    @Test
    fun `completion produced during route recreation is delivered once without replay`() = runTest {
        val events = CredentialResponseResultEvents()
        val completion = CredentialResponseCompletion.Immediate(Intent())

        assertTrue(events.publish(completion))
        assertSame(completion, events.completions.first())
        assertNull(withTimeoutOrNull(1) { events.completions.first() })
    }

    @Test
    fun `cancelled route collector does not consume a completion emitted later`() = runTest {
        val events = CredentialResponseResultEvents()
        val abandonedCollector = async { events.completions.first() }
        abandonedCollector.cancel()
        val completion = CredentialResponseCompletion.Immediate(Intent())

        assertTrue(events.publish(completion))
        assertSame(completion, events.completions.first())
        assertNull(withTimeoutOrNull(1) { events.completions.first() })
    }
}
