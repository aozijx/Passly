package com.aozijx.passly.app.autofill

import java.io.IOException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AutofillServiceStatusObservationTest {
    @Test
    fun `registers before initial snapshot and unregisters when collection stops`() =
        runTest {
            val events = mutableListOf<String>()
            val values =
                observeBooleanStatus(
                    read = {
                        events += "read"
                        true
                    },
                    registerObserver = {
                        events += "register"
                        { events += "unregister" }
                    },
                ).take(1).toList()

            assertEquals(listOf(true), values)
            assertEquals(listOf("register", "read", "unregister"), events)
        }

    @Test
    fun `change callback publishes a fresh snapshot and unregisters after completion`() =
        runTest {
            var enabled = false
            var onChange: (() -> Unit)? = null
            var unregistered = false
            val values = mutableListOf<Boolean>()
            val collection =
                backgroundScope.launch {
                    observeBooleanStatus(
                        read = { enabled },
                        registerObserver = { callback ->
                            onChange = callback
                            { unregistered = true }
                        },
                    ).take(2).toList(values)
                }
            runCurrent()

            enabled = true
            requireNotNull(onChange).invoke()
            runCurrent()
            collection.join()

            assertEquals(listOf(false, true), values)
            assertTrue(unregistered)
        }

    @Test
    fun `read failure closes flow and unregisters observer`() =
        runTest {
            val failure = IOException("status unavailable")
            var unregistered = false
            val result =
                runCatching {
                    observeBooleanStatus(
                        read = { throw failure },
                        registerObserver = { { unregistered = true } },
                    ).toList()
                }

            assertFailure(failure, result.exceptionOrNull())
            assertTrue(unregistered)
        }

    @Test
    fun `read failure from observer callback closes flow and unregisters observer`() =
        runTest {
            val failure = IOException("status callback unavailable")
            var reads = 0
            var onChange: (() -> Unit)? = null
            var unregistered = false
            val result =
                backgroundScope.async {
                    runCatching {
                        observeBooleanStatus(
                            read = {
                                if (++reads == 1) false else throw failure
                            },
                            registerObserver = { callback ->
                                onChange = callback
                                { unregistered = true }
                            },
                        ).toList()
                    }
                }
            runCurrent()

            requireNotNull(onChange).invoke()
            runCurrent()

            assertFailure(failure, result.await().exceptionOrNull())
            assertTrue(unregistered)
        }

    @Test
    fun `registration failure propagates without pretending observation started`() =
        runTest {
            val failure = IOException("observer registration failed")
            val result =
                runCatching {
                    observeBooleanStatus(
                        read = { error("must not read when registration fails") },
                        registerObserver = { throw failure },
                    ).toList()
                }

            assertFailure(failure, result.exceptionOrNull())
        }

    @Test
    fun `fatal read error propagates and still unregisters observer`() =
        runTest {
            val fatal = AssertionError("fatal status failure")
            var unregistered = false
            val result =
                runCatching {
                    observeBooleanStatus(
                        read = { throw fatal },
                        registerObserver = { { unregistered = true } },
                    ).toList()
                }

            assertFailure(fatal, result.exceptionOrNull())
            assertTrue(unregistered)
        }

    @Test
    fun `fatal unregister error propagates when collection is cancelled`() =
        runTest {
            val fatal = AssertionError("fatal cleanup failure")
            val result =
                runCatching {
                    observeBooleanStatus(
                        read = { true },
                        registerObserver = { { throw fatal } },
                    ).take(1).toList()
                }

            assertFailure(fatal, result.exceptionOrNull())
        }

    private fun assertFailure(
        expected: Throwable,
        actual: Throwable?,
    ) {
        requireNotNull(actual)
        assertEquals(expected::class, actual::class)
        assertEquals(expected.message, actual.message)
    }
}
