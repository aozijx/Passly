package com.aozijx.passly.data.local.database.session

import com.aozijx.passly.runtime.session.SessionLockedException
import java.io.IOException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DatabaseObservationRecoveryTest {
    @Test
    fun `session lock race emits placeholder and observation resubscribes after reopen`() =
        runTest {
            val readAccess = MutableStateFlow(DatabaseReadAccess(readable = true, lockGeneration = 0L))
            var starts = 0
            val values = mutableListOf<String>()
            val observation =
                observeDatabaseFlowWhenReadable(
                    readAccess = readAccess,
                    emptyValue = { "locked" },
                ) {
                    starts++
                    if (starts == 1) {
                        flow { throw SessionLockedException("emission raced with seal") }
                    } else {
                        flowOf("reopened")
                    }
                }
            backgroundScope.launch { observation.toList(values) }
            runCurrent()

            assertEquals(listOf("locked"), values)
            readAccess.value = DatabaseReadAccess(readable = true, lockGeneration = 2L)
            runCurrent()

            assertEquals(listOf("locked", "reopened"), values)
            assertEquals(2, starts)
        }

    @Test
    fun `lock cancels a blocked query emission before it can leak stale data`() =
        runTest {
            val readAccess = MutableStateFlow(DatabaseReadAccess(readable = true, lockGeneration = 0L))
            val emissionStarted = CompletableDeferred<Unit>()
            val releaseEmission = CompletableDeferred<Unit>()
            val oldQueryFinished = CompletableDeferred<Unit>()
            var starts = 0
            val values = mutableListOf<String>()
            val observation =
                observeDatabaseFlowWhenReadable(
                    readAccess = readAccess,
                    emptyValue = { "locked" },
                ) {
                    starts++
                    if (starts == 1) {
                        flow {
                            try {
                                emissionStarted.complete(Unit)
                                releaseEmission.await()
                                emit("stale")
                            } finally {
                                oldQueryFinished.complete(Unit)
                            }
                        }
                    } else {
                        flowOf("reopened")
                    }
                }
            backgroundScope.launch { observation.toList(values) }
            runCurrent()
            emissionStarted.await()

            readAccess.value = DatabaseReadAccess(readable = false, lockGeneration = 0L)
            runCurrent()
            oldQueryFinished.await()
            releaseEmission.complete(Unit)
            readAccess.value = DatabaseReadAccess(readable = true, lockGeneration = 1L)
            runCurrent()

            assertEquals(listOf("locked", "reopened"), values)
            assertEquals(2, starts)
        }

    @Test
    fun `ordinary database query failures are not converted to empty values`() =
        runTest {
            val readFailure = IOException("database read failed")
            val result =
                runCatching {
                    observeDatabaseFlowWhenReadable(
                        readAccess = flowOf(DatabaseReadAccess(readable = true, lockGeneration = 0L)),
                        emptyValue = { "locked" },
                    ) {
                        flow<String> { throw readFailure }
                    }.toList()
                }

            assertSame(readFailure, result.exceptionOrNull())
        }
}
