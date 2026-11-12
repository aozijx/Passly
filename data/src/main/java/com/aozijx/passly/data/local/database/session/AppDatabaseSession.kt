package com.aozijx.passly.data.local.database.session

import com.aozijx.passly.core.error.model.DatabaseInitFailed
import com.aozijx.passly.core.observability.LogStatements
import com.aozijx.passly.core.observability.Logger
import com.aozijx.passly.core.observability.Operations
import com.aozijx.passly.core.observability.ReasonCode
import com.aozijx.passly.core.observability.Reasons
import com.aozijx.passly.data.local.database.AppDatabase
import com.aozijx.passly.domain.access.port.SecureSessionAccessState
import com.aozijx.passly.runtime.session.DatabaseSessionLifecycle
import com.aozijx.passly.runtime.session.RuntimeSessionManager
import com.aozijx.passly.runtime.session.SecureSessionState
import com.aozijx.passly.runtime.session.SessionEventSink
import com.aozijx.passly.runtime.session.SessionKeySource
import com.aozijx.passly.runtime.session.SessionStateProvider
import com.aozijx.passly.runtime.session.SessionTransitionEvent
import com.aozijx.passly.runtime.session.SessionTransitionReason
import com.aozijx.passly.runtime.session.SessionUnlockResult
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

/** AppDatabase-typed adapter around the resource-agnostic session runtime. */
@Singleton
class AppDatabaseSession
    @Inject
    internal constructor(
        resource: AppDatabaseSessionResource,
        keySource: SessionKeySource,
        eventLogger: DatabaseSessionEventLogger,
        private val accessState: Provider<SecureSessionAccessState>,
    ) : SessionStateProvider,
        DatabaseSessionLifecycle {
        private val runtime =
            RuntimeSessionManager(
                resource = resource,
                keySource = keySource,
                eventSink = SessionEventSink(eventLogger::record),
            )

        override val lockState: SecureSessionState
            get() = runtime.lockState

        override val lockStateFlow = runtime.lockStateFlow

        override val lockGenerationFlow = runtime.lockGenerationFlow

        /**
         * Checks access before lease acquisition to preserve the caller's denied result, then again
         * inside the lease immediately before the DAO block to close authorization races.
         */
        internal suspend fun <T> queryFullVault(
            onDenied: () -> T,
            block: suspend AppDatabase.() -> T,
        ): T =
            queryWithAccess(
                access = { hasFullSecureSessionAccess() },
                onDenied = onDenied,
                block = block,
            )

        internal suspend fun <T> queryAutofill(
            onDenied: () -> T,
            block: suspend AppDatabase.() -> T,
        ): T =
            queryWithAccess(
                access = { hasAutofillSessionAccess() },
                onDenied = onDenied,
                block = block,
            )

        internal suspend fun <T> transactionFullVault(
            onDenied: () -> T,
            block: suspend AppDatabase.() -> T,
        ): T =
            transactionWithAccess(
                access = { hasFullSecureSessionAccess() },
                onDenied = onDenied,
                block = block,
            )

        internal suspend fun <T> transactionAutofill(
            onDenied: () -> T,
            block: suspend AppDatabase.() -> T,
        ): T =
            transactionWithAccess(
                access = { hasAutofillSessionAccess() },
                onDenied = onDenied,
                block = block,
            )

        internal suspend fun <T> transactionForMaintenance(block: suspend AppDatabase.() -> T): T = runtime.write(block)

        /** Direct read seam for Data integration tests that assert persisted maintenance results. */
        internal suspend fun <T> queryForMaintenance(block: suspend AppDatabase.() -> T): T = runtime.read(block)

        private suspend fun <T> queryWithAccess(
            access: SecureSessionAccessState.() -> Boolean,
            onDenied: () -> T,
            block: suspend AppDatabase.() -> T,
        ): T {
            val currentAccessState = accessState.get()
            if (!currentAccessState.access()) return onDenied()
            return runtime.read {
                if (!currentAccessState.access()) return@read onDenied()
                block()
            }
        }

        private suspend fun <T> transactionWithAccess(
            access: SecureSessionAccessState.() -> Boolean,
            onDenied: () -> T,
            block: suspend AppDatabase.() -> T,
        ): T {
            val currentAccessState = accessState.get()
            if (!currentAccessState.access()) return onDenied()
            return runtime.write {
                if (!currentAccessState.access()) return@write onDenied()
                block()
            }
        }

        internal fun <T> observeFullVaultWhenReadable(
            emptyValue: () -> T,
            query: suspend AppDatabase.() -> Flow<T>,
        ): Flow<T> =
            observeDatabaseFlowWhenReadable(
                readAccess = observeDatabaseReadAccess(accessState.get().isAuthorized, lockStateFlow, lockGenerationFlow),
                emptyValue = emptyValue,
            ) {
                runtime.observe(query)
            }

        override suspend fun unlock(): Throwable? =
            when (val result = runtime.unlock()) {
                SessionUnlockResult.Success -> {
                    null
                }

                is SessionUnlockResult.KeyUnavailable -> {
                    DatabaseInitFailed(
                        throwableType = result.cause.javaClass.simpleName,
                    )
                }

                is SessionUnlockResult.OpenFailed -> {
                    result.cause
                }
            }

        override suspend fun softLock() = runtime.softLock()

        override suspend fun seal() = runtime.seal()
    }

internal class DatabaseSessionEventLogger
    @Inject
    constructor(
        private val logger: Logger,
    ) {
        suspend fun record(event: SessionTransitionEvent) {
            logger.inSpan(Operations.DatabaseSession) {
                val reason = event.reason.toReasonCode()
                val logFields =
                    LogStatements.Database.sessionFields(
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
                    SessionTransitionReason.OPEN_FAILED -> {
                        logger.log(
                            LogStatements.Database.sessionOpenFailed(
                                requireNotNull(event.error),
                                reason,
                                logFields,
                            ),
                        )
                    }

                    SessionTransitionReason.CLOSE_FAILED -> {
                        logger.log(
                            LogStatements.Database.sessionCloseFailed(
                                requireNotNull(event.error),
                                reason,
                                logFields,
                            ),
                        )
                    }

                    SessionTransitionReason.DRAIN_TIMEOUT -> {
                        logger.log(
                            LogStatements.Database.sessionDrainTimeout(reason, logFields),
                        )
                    }

                    else -> {
                        logger.log(LogStatements.Database.sessionTransition(logFields))
                    }
                }
            }
        }

        private fun SessionTransitionReason.toReasonCode(): ReasonCode =
            when (this) {
                SessionTransitionReason.OPEN -> Reasons.Database.Session.SessionOpen
                SessionTransitionReason.OPEN_FAILED -> Reasons.Database.Session.SessionOpenFailed
                SessionTransitionReason.RESUME -> Reasons.Database.Session.SessionResume
                SessionTransitionReason.SOFT_LOCK -> Reasons.Database.Session.SessionSoftLock
                SessionTransitionReason.SEAL -> Reasons.Database.Session.SessionSeal
                SessionTransitionReason.DRAIN_TIMEOUT -> Reasons.Database.Session.SessionDrainTimeout
                SessionTransitionReason.CLOSE_FAILED -> Reasons.Database.Resource.DatabaseResourceCloseFailed
            }
    }
