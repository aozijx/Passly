package com.aozijx.passly.runtime.session

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Owns the secure resource state machine without knowing the concrete database or key manager.
 */
class RuntimeSessionManager<Handle : Any>(
    resource: SessionResource<Handle>,
    private val keySource: SessionKeySource,
    private val eventSink: SessionEventSink = SessionEventSink.None,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : SessionStateProvider {
    private val leaseGate = SessionLeaseGate(resource, eventSink)

    override val lockState: SecureSessionState
        get() = leaseGate.lockState.value

    override val lockStateFlow = leaseGate.lockState

    suspend fun <T> read(block: suspend Handle.() -> T): T = leaseGate.read(block)

    suspend fun <T> write(block: suspend Handle.() -> T): T = leaseGate.write(block)

    fun <T> observe(block: suspend Handle.() -> Flow<T>): Flow<T> = leaseGate.observe(block)

    suspend fun unlock(): SessionUnlockResult {
        if (leaseGate.resumeIfOpen()) return SessionUnlockResult.Success

        return withContext(ioDispatcher) {
            val key = try {
                keySource.copyKey()
            } catch (error: Throwable) {
                return@withContext SessionUnlockResult.KeyUnavailable(error)
            }
            try {
                leaseGate.open(key)
            } finally {
                key.fill(0)
            }
        }
    }

    suspend fun softLock() = leaseGate.softLock()

    suspend fun seal(timeout: Duration = 5.seconds) = leaseGate.seal(timeout)

    suspend fun closeResource() = leaseGate.close()
}
