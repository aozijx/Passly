package com.aozijx.passly.app.settings

import com.aozijx.passly.domain.settings.port.CorruptSettingsDataException
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withTimeoutOrNull

sealed interface RecoverableReadState<out T> {
    data object Loading : RecoverableReadState<Nothing>

    data class Ready<T>(
        val value: T,
    ) : RecoverableReadState<T>

    data class Failed(
        val failure: ReadFailure,
    ) : RecoverableReadState<Nothing>
}

enum class ReadFailure {
    IO,
    CORRUPT_DATA,
    UNEXPECTED,
}

private class SourceReadFailure(
    val sourceCause: Exception,
    val localRetryVersion: Long,
    val sharedRetryVersion: Long?,
) : Exception(sourceCause)

/** Owns collection and recovery for one cold repository observation. */
class RecoverableRead<T>(
    source: Flow<T>,
    scope: CoroutineScope,
    private val retryDelayMillis: Long = DEFAULT_RETRY_DELAY_MILLIS,
    private val sharedRetrySignal: SettingsReadRetrySignal? = null,
) {
    private val retryRequests = MutableStateFlow(0L)

    val updates: Flow<RecoverableReadState<T>> =
        flow {
            val localRetryVersion = retryRequests.value
            val sharedRetryVersion = sharedRetrySignal?.version?.value
            emit(RecoverableReadState.Loading)
            var sourceFailure: Exception? = null
            source
                .catch { cause ->
                    if (cause is CancellationException || cause !is Exception) throw cause
                    sourceFailure = cause
                }.collect { emit(RecoverableReadState.Ready(it)) }
            sourceFailure?.let { throw SourceReadFailure(it, localRetryVersion, sharedRetryVersion) }
        }.retryWhen { cause, attempt ->
            val sourceFailure = cause as? SourceReadFailure ?: throw cause
            val failure = sourceFailure.sourceCause
            when {
                failure is CorruptSettingsDataException -> {
                    emit(RecoverableReadState.Failed(ReadFailure.CORRUPT_DATA))
                    awaitRetry(sourceFailure.localRetryVersion, sourceFailure.sharedRetryVersion)
                    true
                }

                failure is IOException -> {
                    emit(RecoverableReadState.Failed(ReadFailure.IO))
                    withTimeoutOrNull(retryDelay(attempt)) {
                        awaitRetry(sourceFailure.localRetryVersion, sourceFailure.sharedRetryVersion)
                    }
                    true
                }

                else -> {
                    emit(RecoverableReadState.Failed(ReadFailure.UNEXPECTED))
                    awaitRetry(sourceFailure.localRetryVersion, sourceFailure.sharedRetryVersion)
                    true
                }
            }
        }

    val state: StateFlow<RecoverableReadState<T>> =
        updates.stateIn(
            scope = scope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = RecoverableReadState.Loading,
        )

    init {
        require(retryDelayMillis > 0)
    }

    fun retry() {
        retryRequests.value++
        sharedRetrySignal?.request()
    }

    private suspend fun awaitRetry(
        localVersion: Long,
        sharedVersion: Long?,
    ) {
        val sharedRequests = sharedRetrySignal?.version
        if (sharedRequests == null || sharedVersion == null) {
            retryRequests.first { it != localVersion }
        } else {
            combine(retryRequests, sharedRequests) { local, shared ->
                local != localVersion || shared != sharedVersion
            }.first { it }
        }
    }

    private fun retryDelay(attempt: Long): Long {
        val multiplier = 1L shl attempt.coerceIn(0L, 6L).toInt()
        return (retryDelayMillis.coerceAtMost(MAX_RETRY_DELAY_MILLIS / multiplier) * multiplier)
            .coerceAtMost(MAX_RETRY_DELAY_MILLIS)
    }

    private companion object {
        const val DEFAULT_RETRY_DELAY_MILLIS = 1_000L
        const val MAX_RETRY_DELAY_MILLIS = 60_000L
    }
}
