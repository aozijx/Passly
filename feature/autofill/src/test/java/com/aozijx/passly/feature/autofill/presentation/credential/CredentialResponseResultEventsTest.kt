package com.aozijx.passly.feature.autofill.presentation.credential

import android.content.Intent
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class CredentialResponseResultEventsTest {
    @Test
    fun `abandoning the host discards a result buffered while stopped`() =
        runTest {
            val events = CredentialResponseResultEvents()
            assertTrue(events.publish(CredentialResponseCompletion.Immediate(Intent())))

            events.discard()

            assertFalse(events.publish(CredentialResponseCompletion.Immediate(Intent())))
            assertTrue(runCatching { events.completions.first() }.exceptionOrNull() is CancellationException)
        }

    @Test
    fun `completion survives route collector gaps and is delivered once without replay`() =
        runTest {
            val recreatedRouteEvents = CredentialResponseResultEvents()
            val bufferedCompletion = CredentialResponseCompletion.Immediate(Intent())

            assertTrue(recreatedRouteEvents.publish(bufferedCompletion))
            assertSame(bufferedCompletion, recreatedRouteEvents.completions.first())
            assertNull(withTimeoutOrNull(1) { recreatedRouteEvents.completions.first() })

            val cancelledRouteEvents = CredentialResponseResultEvents()
            val abandonedCollector =
                async(start = CoroutineStart.UNDISPATCHED) {
                    cancelledRouteEvents.completions.first()
                }
            assertTrue(abandonedCollector.isActive)
            abandonedCollector.cancel()
            val laterCompletion = CredentialResponseCompletion.Immediate(Intent())

            assertTrue(cancelledRouteEvents.publish(laterCompletion))
            assertSame(laterCompletion, cancelledRouteEvents.completions.first())
            assertNull(withTimeoutOrNull(1) { cancelledRouteEvents.completions.first() })
        }
}
