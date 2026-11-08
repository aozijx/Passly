package com.aozijx.passly.data.local.datastore

import androidx.datastore.core.CorruptionException
import com.aozijx.passly.domain.settings.port.CorruptSettingsDataException
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertSame
import org.junit.Test

class SettingsDataStoreReadTest {
    @Test
    fun `protobuf corruption is tagged without substituting a value`() =
        runTest {
            val cause = CorruptionException("invalid protobuf")
            val observed =
                try {
                    flow<Int> { throw cause }.tagCorruptSettingsData().first()
                    null
                } catch (error: CorruptSettingsDataException) {
                    error
                }

            assertSame(cause, observed?.original)
        }

    @Test
    fun `io failures cancellation and fatal errors keep their original meaning`() =
        runTest {
            val ioFailure = IOException("unavailable")
            val observedIo =
                try {
                    flow<Int> { throw ioFailure }.tagCorruptSettingsData().first()
                    null
                } catch (error: IOException) {
                    error
                }
            val cancellation = CancellationException("cancelled")
            val observedCancellation =
                try {
                    flow<Int> { throw cancellation }.tagCorruptSettingsData().first()
                    null
                } catch (error: CancellationException) {
                    error
                }
            val fatalError = AssertionError("fatal")
            val observedFatal =
                try {
                    flow<Int> { throw fatalError }.tagCorruptSettingsData().first()
                    null
                } catch (error: AssertionError) {
                    error
                }

            assertSame(ioFailure, observedIo)
            assertSame(cancellation, observedCancellation)
            assertSame(fatalError, observedFatal)
        }
}
