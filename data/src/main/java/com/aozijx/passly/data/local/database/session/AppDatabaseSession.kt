package com.aozijx.passly.data.local.database.session

import com.aozijx.passly.core.error.model.DatabaseInitFailed
import com.aozijx.passly.core.observability.LogStatements
import com.aozijx.passly.core.observability.Logger
import com.aozijx.passly.core.observability.Operations
import com.aozijx.passly.core.observability.ReasonCode
import com.aozijx.passly.core.observability.Reasons
import com.aozijx.passly.data.local.database.AppDatabase
import com.aozijx.passly.runtime.session.DatabaseSessionLifecycle
import com.aozijx.passly.runtime.session.RuntimeSessionManager
import com.aozijx.passly.runtime.session.SecureSessionState
import com.aozijx.passly.runtime.session.SessionEventSink
import com.aozijx.passly.runtime.session.SessionKeySource
import com.aozijx.passly.runtime.session.SessionStateProvider
import com.aozijx.passly.runtime.session.SessionTransitionEvent
import com.aozijx.passly.runtime.session.SessionTransitionReason
import com.aozijx.passly.runtime.session.SessionUnlockResult
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/** AppDatabase-typed adapter around the resource-agnostic session runtime. */
@Singleton
class AppDatabaseSession @Inject internal constructor(
    resource: AppDatabaseSessionResource,
    keySource: SessionKeySource,
    eventLogger: DatabaseSessionEventLogger,
) : SessionStateProvider, DatabaseSessionLifecycle {
    private val runtime = RuntimeSessionManager(
        resource = resource,
        keySource = keySource,
        eventSink = SessionEventSink(eventLogger::record),
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

    override suspend fun seal() = runtime.seal()
}

internal class DatabaseSessionEventLogger @Inject constructor(
    private val logger: Logger,
) {
    suspend fun record(event: SessionTransitionEvent) {
        logger.inSpan(Operations.DatabaseSession) {
            val reason = event.reason.toReasonCode()
            val logFields = LogStatements.Database.sessionFields(
                fromState = event.fromState,
                toState = event.toState,
                lockType = event.lockType,
                reason = reason,
                owner = event.owner,
                elapsedMs = event.elapsedMs,
                timeoutMs = event.timeoutMs,
                activeLeaseCount = event.activeLeaseCount,
            )
            when (event.reason) {
                SessionTransitionReason.OPEN_FAILED -> logger.log(
                    LogStatements.Database.sessionOpenFailed(
                        requireNotNull(event.error),
                        reason,
                        logFields,
                    ),
                )
                SessionTransitionReason.CLOSE_FAILED -> logger.log(
                    LogStatements.Database.sessionCloseFailed(
                        requireNotNull(event.error),
                        reason,
                        logFields,
                    ),
                )
                SessionTransitionReason.DRAIN_TIMEOUT -> logger.log(
                    LogStatements.Database.sessionDrainTimeout(reason, logFields),
                )
                else -> logger.log(LogStatements.Database.sessionTransition(logFields))
            }
        }
    }

    private fun SessionTransitionReason.toReasonCode(): ReasonCode = when (this) {
        SessionTransitionReason.OPEN -> Reasons.SessionOpen
        SessionTransitionReason.OPEN_FAILED -> Reasons.SessionOpenFailed
        SessionTransitionReason.RESUME -> Reasons.SessionResume
        SessionTransitionReason.SOFT_LOCK -> Reasons.SessionSoftLock
        SessionTransitionReason.SEAL -> Reasons.SessionSeal
        SessionTransitionReason.DRAIN_TIMEOUT -> Reasons.SessionDrainTimeout
        SessionTransitionReason.CLOSE_FAILED -> Reasons.DatabaseResourceCloseFailed
    }
}
