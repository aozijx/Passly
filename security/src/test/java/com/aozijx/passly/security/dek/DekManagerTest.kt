package com.aozijx.passly.security.dek

import com.aozijx.passly.core.observability.Events
import com.aozijx.passly.core.observability.LogStatement
import com.aozijx.passly.core.observability.Logger
import com.aozijx.passly.domain.access.model.EnvelopeType
import com.aozijx.passly.testing.testLogger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class DekManagerTest {
    @Test
    fun `lock wipes the cached DEK before publishing locked`() =
        runBlocking {
            val fieldKeyManager = FieldKeyManager()
            val manager = DekManager(InMemoryDekBootstrapStore(), fieldKeyManager, testLogger())
            assertSame(DekUnlockResult.Success, manager.setDek(EnvelopeType.APP_PASSWORD, ByteArray(32) { 7 }))
            val cachedDek = (manager.state as DekState.Unlocked).dek

            manager.lock()

            assertArrayEquals(ByteArray(cachedDek.size), cachedDek)
            assertSame(DekState.Locked, manager.state)
            assertFalse(manager.isUnlocked.value)
            assertFieldKeyUnavailable(fieldKeyManager)

            manager.lock()
            assertSame(DekState.Locked, manager.state)
            assertSame(DekUnlockResult.Success, manager.setDek(EnvelopeType.APP_PASSWORD, ByteArray(32) { 7 }))
            assertTrue(manager.isUnlocked.value)
        }

    @Test
    fun `lock log exceptions cancellation and fatal errors finish erasure and publish locked`() =
        runBlocking {
            val failures =
                listOf(
                    IllegalStateException("lock-start log failed"),
                    CancellationException("lock-start logging cancelled"),
                    AssertionError("fatal lock-start logging failure"),
                )
            failures.forEach { logFailure ->
                val logger = LockStartFailingLogger(logFailure)
                val fieldKeyManager = FieldKeyManager()
                val manager = DekManager(InMemoryDekBootstrapStore(), fieldKeyManager, logger)
                assertSame(DekUnlockResult.Success, manager.setDek(EnvelopeType.APP_PASSWORD, ByteArray(32) { 8 }))
                val cachedDek = (manager.state as DekState.Unlocked).dek
                logger.armed = true

                val observedFailure =
                    withContext(NonCancellable) {
                        runCatching { manager.lock() }.exceptionOrNull()
                    }

                assertEquals(logFailure::class, observedFailure!!::class)
                assertEquals(logFailure.message, observedFailure.message)
                assertArrayEquals(ByteArray(cachedDek.size), cachedDek)
                assertSame(DekState.Locked, manager.state)
                assertFalse(manager.isUnlocked.value)
                assertFieldKeyUnavailable(fieldKeyManager)
            }
        }

    @Test
    fun `field key derivation failure does not publish unlocked DEK and wipes input`() =
        runBlocking {
            val fieldKeyManager = FailingFieldKeyLifecycle()
            val manager = DekManager(InMemoryDekBootstrapStore(), fieldKeyManager, testLogger())
            val inputDek = ByteArray(32) { 9 }
            fieldKeyManager.failDerivation = true

            val result = manager.setDek(EnvelopeType.APP_PASSWORD, inputDek)

            assertTrue(result is DekUnlockResult.Failed)
            assertSame(DekState.Locked, manager.state)
            assertFalse(manager.isUnlocked.value)
            assertArrayEquals(ByteArray(inputDek.size), inputDek)
            assertFalse(fieldKeyManager.hasKey)
        }

    @Test
    fun `field key derivation cancellation propagates after restoring the locked state`() =
        runBlocking {
            val fieldKeyManager = FailingFieldKeyLifecycle()
            val injectedFailure = CancellationException("field key derivation cancelled")
            fieldKeyManager.derivationFailure = injectedFailure
            val manager = DekManager(InMemoryDekBootstrapStore(), fieldKeyManager, testLogger())
            val inputDek = ByteArray(32) { 4 }

            val failure =
                withContext(NonCancellable) {
                    runCatching { manager.setDek(EnvelopeType.APP_PASSWORD, inputDek) }.exceptionOrNull()
                }

            assertSame(injectedFailure, failure)
            assertSame(DekState.Locked, manager.state)
            assertFalse(manager.isUnlocked.value)
            assertArrayEquals(ByteArray(inputDek.size), inputDek)
            assertFalse(fieldKeyManager.hasKey)
        }

    private fun assertFieldKeyUnavailable(fieldKeyManager: FieldKeyManager) {
        val failure = runCatching { fieldKeyManager.copyKey() }.exceptionOrNull()
        assertEquals(IllegalStateException::class, failure!!::class)
    }
    private class FailingFieldKeyLifecycle : FieldKeyLifecycle {
        var failDerivation = false
        var derivationFailure: Throwable? = null
        var hasKey = false
            private set

        override fun deriveAndSet(dek: ByteArray) {
            derivationFailure?.let { throw it }
            if (failDerivation) throw IllegalStateException("field key derivation failed")
            hasKey = true
        }

        override fun clear() {
            hasKey = false
        }
    }

    private class LockStartFailingLogger(
        private val failure: Throwable,
    ) : Logger by testLogger() {
        var armed = false

        override fun log(statement: LogStatement) {
            if (armed && statement.event == Events.Security.VaultLockStarted) {
                armed = false
                throw failure
            }
            statement.writeTo(this)
        }
    }
}
