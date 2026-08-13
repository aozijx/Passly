package com.aozijx.passly.data.local.database.session

import com.aozijx.passly.core.error.model.DatabaseInitFailed
import com.aozijx.passly.core.telemetry.EventCategory
import com.aozijx.passly.core.telemetry.EventLevel
import com.aozijx.passly.core.telemetry.TelemetryEvent
import com.aozijx.passly.core.telemetry.TelemetryReporter
import com.aozijx.passly.data.local.database.AppDatabase
import com.aozijx.passly.domain.authentication.SecureSessionState
import com.aozijx.passly.domain.authentication.DatabaseSessionLifecycle
import com.aozijx.passly.domain.authentication.SessionStateProvider
import com.aozijx.passly.runtime.session.RuntimeSessionManager
import com.aozijx.passly.runtime.session.SessionEventSink
import com.aozijx.passly.runtime.session.SessionKeySource
import com.aozijx.passly.runtime.session.SessionRuntimeEvent
import com.aozijx.passly.runtime.session.SessionUnlockResult
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/** AppDatabase-typed adapter around the resource-agnostic session runtime. */
@Singleton
internal class UnifiedSessionManager @Inject constructor(
    resource: AppDatabaseSessionResource,
    keySource: SessionKeySource,
    private val telemetry: TelemetryReporter,
) : SessionStateProvider, DatabaseSessionLifecycle {
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

    override suspend fun unlock(): Throwable? = when (val result = runtime.unlock()) {
        SessionUnlockResult.Success -> null
        is SessionUnlockResult.KeyUnavailable -> DatabaseInitFailed(
            throwableType = result.cause.javaClass.simpleName,
        )
        is SessionUnlockResult.OpenFailed -> result.cause
    }

    override suspend fun softLock() = runtime.softLock()

    override suspend fun seal() = runtime.seal(5.seconds)

    suspend fun seal(timeout: Duration) = runtime.seal(timeout)

    suspend fun closeDatabase() = runtime.closeResource()

    private fun recordRuntimeEvent(event: SessionRuntimeEvent, error: Throwable?) {
        when (event) {
            SessionRuntimeEvent.OPEN_FAILED,
            SessionRuntimeEvent.CLOSE_FAILED,
            -> emitRuntimeEvent(EventLevel.ERROR, event, error)

            SessionRuntimeEvent.SEAL_DRAIN_TIMEOUT -> emitRuntimeEvent(EventLevel.WARN, event)
            else -> emitRuntimeEvent(EventLevel.INFO, event)
        }
    }

    private fun emitRuntimeEvent(
        level: EventLevel,
        event: SessionRuntimeEvent,
        error: Throwable? = null,
    ) {
        telemetry.emit(
            TelemetryEvent(
                level = level,
                category = EventCategory.DATABASE,
                name = "database.session.${event.name.lowercase()}",
                throwableType = error?.javaClass?.simpleName,
            )
        )
    }
}
