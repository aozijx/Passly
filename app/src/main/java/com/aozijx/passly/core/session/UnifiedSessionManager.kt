package com.aozijx.passly.core.session

import com.aozijx.passly.app.diagnostics.AppTelemetry
import com.aozijx.passly.core.error.model.DatabaseInitFailed
import com.aozijx.passly.data.local.database.AppDatabase
import com.aozijx.passly.domain.authentication.SecureSessionState
import com.aozijx.passly.domain.authentication.SessionStateProvider
import com.aozijx.passly.runtime.session.RuntimeSessionManager
import com.aozijx.passly.runtime.session.SessionEventSink
import com.aozijx.passly.runtime.session.SessionRuntimeEvent
import com.aozijx.passly.runtime.session.SessionUnlockResult
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/** AppDatabase-typed adapter around the resource-agnostic session runtime. */
@Singleton
class UnifiedSessionManager @Inject internal constructor(
    resource: AppDatabaseSessionResource,
    keySource: DekSessionKeySource,
) : SessionStateProvider {
    private companion object {
        const val TAG = "UnifiedSessionManager"
    }

    private val runtime = RuntimeSessionManager(
        resource = resource,
        keySource = keySource,
        eventSink = SessionEventSink(::recordRuntimeEvent),
    )

    override val lockState: SecureSessionState
        get() = runtime.lockState

    override val lockStateFlow = runtime.lockStateFlow

    suspend fun <T> query(block: suspend AppDatabase.() -> T): T = runtime.read(block)

    suspend fun <T> transaction(block: suspend AppDatabase.() -> T): T = runtime.write(block)

    fun <T> observeFlow(block: suspend AppDatabase.() -> Flow<T>): Flow<T> =
        runtime.observe(block)

    suspend fun unlock(): Throwable? = when (val result = runtime.unlock()) {
        SessionUnlockResult.Success -> null
        is SessionUnlockResult.KeyUnavailable -> DatabaseInitFailed(
            throwableType = result.cause.javaClass.simpleName,
        )
        is SessionUnlockResult.OpenFailed -> result.cause
    }

    suspend fun softLock() = runtime.softLock()

    suspend fun seal(timeout: Duration = 5.seconds) = runtime.seal(timeout)

    suspend fun closeDatabase() = runtime.closeResource()

    private fun recordRuntimeEvent(event: SessionRuntimeEvent, error: Throwable?) {
        when (event) {
            SessionRuntimeEvent.OPEN_FAILED,
            SessionRuntimeEvent.CLOSE_FAILED,
            -> AppTelemetry.e(TAG, event.name, error)

            SessionRuntimeEvent.SEAL_DRAIN_TIMEOUT -> AppTelemetry.w(TAG, event.name)
            else -> AppTelemetry.i(TAG, event.name)
        }
    }
}
