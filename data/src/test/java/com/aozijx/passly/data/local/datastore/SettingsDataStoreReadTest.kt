package com.aozijx.passly.data.local.datastore

import androidx.datastore.core.CorruptionException
import com.aozijx.passly.domain.settings.port.CorruptSettingsDataException
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsDataStoreReadTest {
    @Test
    fun `protobuf corruption is tagged without substituting a value`() =
        runTest {
            val cause = CorruptionException("invalid protobuf")
            var observed: Throwable? = null
            try {
                flow<Int> { throw cause }.tagCorruptSettingsData().first()
            } catch (error: Throwable) {
                observed = error
            }

            assertTrue(observed is CorruptSettingsDataException)
            assertSame(cause, (observed as CorruptSettingsDataException).original)
        }

    @Test
    fun `io failures and cancellation keep their original meaning`() =
        runTest {
            val ioFailure = IOException("unavailable")
            var observedIo: Throwable? = null
            try {
                flow<Int> { throw ioFailure }.tagCorruptSettingsData().first()
            } catch (error: Throwable) {
                observedIo = error
            }
            val cancellation = CancellationException("cancelled")
            var observedCancellation: Throwable? = null
            try {
                flow<Int> { throw cancellation }.tagCorruptSettingsData().first()
            } catch (error: Throwable) {
                observedCancellation = error
            }

            assertSame(ioFailure, observedIo)
            assertSame(cancellation, observedCancellation)
        }
}
