package com.aozijx.passly.data.repository.entry.command

import com.aozijx.passly.core.error.result.AppResult
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Serializes revision-history generation checks and the durable rebuild/cleanup sequence. */
internal class EntryRevisionHistoryInitializationFlow
    constructor(
        private val currentGeneration: Int,
        private val completedGeneration: suspend () -> AppResult<Int>,
        private val rebuild: suspend () -> AppResult<Unit>,
        private val markCompleted: suspend (Int) -> AppResult<Unit>,
        private val afterCommitCleanup: suspend () -> Unit,
    ) {
        private val mutex = Mutex()

        suspend fun execute(): AppResult<Unit> =
            mutex.withLock {
                when (val completed = completedGeneration()) {
                    is AppResult.Failure -> {
                        completed
                    }

                    is AppResult.Success -> {
                        if (completed.data >= currentGeneration) {
                            AppResult.success(Unit)
                        } else {
                            when (val rebuilt = rebuild()) {
                                is AppResult.Failure -> rebuilt

                                is AppResult.Success -> {
                                    when (val marked = markCompleted(currentGeneration)) {
                                        is AppResult.Failure -> marked
                                        is AppResult.Success -> {
                                            afterCommitCleanup()
                                            AppResult.success(Unit)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
    }
