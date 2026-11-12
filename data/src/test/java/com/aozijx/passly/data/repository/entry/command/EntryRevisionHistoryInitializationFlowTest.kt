package com.aozijx.passly.data.repository.entry.command

import com.aozijx.passly.core.error.model.SessionModeRestricted
import com.aozijx.passly.core.error.result.AppResult
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class EntryRevisionHistoryInitializationFlowTest {
    @Test
    fun completedGenerationSkipsRebuildAndCleanup() =
        runTest {
            var rebuilds = 0
            var cleanupRuns = 0
            val flow =
                initializationFlow(
                    completedGeneration = { AppResult.success(1) },
                    rebuild = {
                        rebuilds++
                        AppResult.success(Unit)
                    },
                    afterCommitCleanup = { cleanupRuns++ },
                )

            assertEquals(AppResult.success(Unit), flow.execute())
            assertEquals(0, rebuilds)
            assertEquals(0, cleanupRuns)
        }

    @Test
    fun durableRebuildMarksGenerationBeforeCleanupAndSkipsLaterRebuilds() =
        runTest {
            var completedGeneration = 0
            var rebuilds = 0
            val events = mutableListOf<String>()
            val flow =
                initializationFlow(
                    completedGeneration = {
                        events += "read"
                        AppResult.success(completedGeneration)
                    },
                    rebuild = {
                        events += "transaction"
                        rebuilds++
                        AppResult.success(Unit)
                    },
                    markCompleted = { generation ->
                        events += "mark:$generation"
                        completedGeneration = generation
                        AppResult.success(Unit)
                    },
                    afterCommitCleanup = { events += "cleanup" },
                )

            assertEquals(AppResult.success(Unit), flow.execute())
            assertEquals(AppResult.success(Unit), flow.execute())

            assertEquals(listOf("read", "transaction", "mark:1", "cleanup", "read"), events)
            assertEquals(1, rebuilds)
        }

    @Test
    fun rebuildFailureDoesNotMarkGenerationAndNextCallRetries() =
        runTest {
            val rebuildFailure = AppResult.failure(SessionModeRestricted())
            var rebuilds = 0
            var marks = 0
            var cleanupRuns = 0
            val flow =
                initializationFlow(
                    completedGeneration = { AppResult.success(0) },
                    rebuild = {
                        rebuilds++
                        if (rebuilds == 1) rebuildFailure else AppResult.success(Unit)
                    },
                    markCompleted = { generation ->
                        marks++
                        assertEquals(1, generation)
                        AppResult.success(Unit)
                    },
                    afterCommitCleanup = { cleanupRuns++ },
                )

            assertSame(rebuildFailure, flow.execute())
            assertEquals(0, marks)
            assertEquals(0, cleanupRuns)
            assertEquals(AppResult.success(Unit), flow.execute())
            assertEquals(2, rebuilds)
            assertEquals(1, marks)
            assertEquals(1, cleanupRuns)
        }

    @Test
    fun generationMarkFailureRetriesDurableRebuildBeforeCleanup() =
        runTest {
            val markFailure = AppResult.failure(SessionModeRestricted())
            var completedGeneration = 0
            var rebuilds = 0
            var marks = 0
            var cleanupRuns = 0
            val flow =
                initializationFlow(
                    completedGeneration = { AppResult.success(completedGeneration) },
                    rebuild = {
                        rebuilds++
                        AppResult.success(Unit)
                    },
                    markCompleted = { generation ->
                        marks++
                        if (marks == 1) {
                            markFailure
                        } else {
                            completedGeneration = generation
                            AppResult.success(Unit)
                        }
                    },
                    afterCommitCleanup = { cleanupRuns++ },
                )

            assertSame(markFailure, flow.execute())
            assertEquals(0, cleanupRuns)
            assertEquals(AppResult.success(Unit), flow.execute())

            assertEquals(2, rebuilds)
            assertEquals(2, marks)
            assertEquals(1, cleanupRuns)
        }

    @Test
    fun concurrentCallsRecheckGenerationAfterFirstTransactionCommits() =
        runTest {
            var completedGeneration = 0
            var reads = 0
            var rebuilds = 0
            val rebuildStarted = CompletableDeferred<Unit>()
            val finishRebuild = CompletableDeferred<Unit>()
            val firstResult = CompletableDeferred<AppResult<Unit>>()
            val secondResult = CompletableDeferred<AppResult<Unit>>()
            val flow =
                initializationFlow(
                    completedGeneration = {
                        reads++
                        AppResult.success(completedGeneration)
                    },
                    rebuild = {
                        rebuilds++
                        rebuildStarted.complete(Unit)
                        finishRebuild.await()
                        AppResult.success(Unit)
                    },
                    markCompleted = { generation ->
                        completedGeneration = generation
                        AppResult.success(Unit)
                    },
                    afterCommitCleanup = {},
                )

            launch { firstResult.complete(flow.execute()) }
            rebuildStarted.await()
            launch { secondResult.complete(flow.execute()) }
            runCurrent()

            assertEquals(1, reads)
            finishRebuild.complete(Unit)
            runCurrent()

            assertEquals(AppResult.success(Unit), firstResult.await())
            assertEquals(AppResult.success(Unit), secondResult.await())
            assertEquals(2, reads)
            assertEquals(1, rebuilds)
        }

    private fun initializationFlow(
        completedGeneration: suspend () -> AppResult<Int>,
        rebuild: suspend () -> AppResult<Unit>,
        markCompleted: suspend (Int) -> AppResult<Unit> = { AppResult.success(Unit) },
        afterCommitCleanup: suspend () -> Unit = {},
    ) =
        EntryRevisionHistoryInitializationFlow(
            currentGeneration = 1,
            completedGeneration = completedGeneration,
            rebuild = rebuild,
            markCompleted = markCompleted,
            afterCommitCleanup = afterCommitCleanup,
        )
}
