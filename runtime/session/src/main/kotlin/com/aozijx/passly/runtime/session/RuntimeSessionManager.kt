package com.aozijx.passly.runtime.session

import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Owns the secure resource state machine without knowing the concrete database or key manager.
 */
class RuntimeSessionManager<Handle : Any>(
    resource: SessionResource<Handle>,
    private val keySource: SessionKeySource,
    private val eventSink: SessionEventSink = SessionEventSink.None,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : SessionStateProvider {
    private val leaseGate = SessionLeaseGate(resource, eventSink, ioDispatcher)
    private val lifecycleMutex = Mutex()

    override val lockState: SecureSessionState
        get() = leaseGate.lockState.value

    override val lockStateFlow = leaseGate.lockState

    override val lockGenerationFlow = leaseGate.lockGeneration

    suspend fun <T> read(block: suspend Handle.() -> T): T = leaseGate.read(block)

    suspend fun <T> write(block: suspend Handle.() -> T): T = leaseGate.write(block)

    fun <T> observe(block: suspend Handle.() -> Flow<T>): Flow<T> = leaseGate.observe(block)

    suspend fun unlock(): SessionUnlockResult =
        lifecycleMutex.withLock {
            leaseGate.closeFailure()?.let { return@withLock SessionUnlockResult.OpenFailed(it) }
            if (leaseGate.resumeIfOpen()) return@withLock SessionUnlockResult.Success

            var fatalError: Error? = null
            val result =
                withContext(ioDispatcher) {
                    var key: ByteArray? = null
                    try {
                        key =
                            try {
                                keySource.copyKey()
                            } catch (cancelled: CancellationException) {
                                throw cancelled
                            } catch (error: Error) {
                                fatalError = error
                                return@withContext SessionUnlockResult.OpenFailed(error)
                            } catch (error: Exception) {
                                return@withContext SessionUnlockResult.KeyUnavailable(error)
                            }
                        try {
                            leaseGate.open(requireNotNull(key))
                        } catch (error: Error) {
                            fatalError = error
                            SessionUnlockResult.OpenFailed(error)
                        }
                    } finally {
                        key?.fill(0)
                    }
                }
            fatalError?.let { throw it }
            result
        }

    suspend fun softLock(timeout: Duration = 5.seconds) = lifecycleMutex.withLock { leaseGate.softLock(timeout) }

    suspend fun seal(timeout: Duration = 5.seconds): Result<Unit> = lifecycleMutex.withLock { leaseGate.seal(timeout) }
}
