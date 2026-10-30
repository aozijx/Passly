package com.aozijx.passly.data.local.database.session

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.aozijx.passly.core.observability.ErrorCode
import com.aozijx.passly.core.observability.EventKey
import com.aozijx.passly.core.observability.LogFields
import com.aozijx.passly.core.observability.Logger
import com.aozijx.passly.core.observability.OperationCode
import com.aozijx.passly.core.observability.ReasonCode
import com.aozijx.passly.core.observability.SpanId
import com.aozijx.passly.core.observability.TraceId
import com.aozijx.passly.core.observability.TraceResumeResult
import com.aozijx.passly.core.observability.TraceScope
import com.aozijx.passly.core.observability.trace.TraceToken
import com.aozijx.passly.data.local.database.DatabaseProvider
import com.aozijx.passly.data.local.database.DatabaseSchema
import com.aozijx.passly.data.local.database.entity.EntryActivityEntity
import com.aozijx.passly.data.local.database.entity.EntryEntity
import com.aozijx.passly.data.local.database.entity.EntryLinkEntity
import com.aozijx.passly.data.local.database.entity.EntryRevisionEntity
import com.aozijx.passly.domain.access.model.AuthenticationState
import com.aozijx.passly.domain.access.port.SecureSessionAccessState
import com.aozijx.passly.domain.entry.model.activity.ActivityType
import com.aozijx.passly.domain.entry.model.relation.EntryRelationType
import com.aozijx.passly.runtime.session.SecureSessionState
import com.aozijx.passly.runtime.session.SessionLockedException
import com.aozijx.passly.runtime.session.SessionKeySource
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.runBlocking
import java.util.concurrent.atomic.AtomicInteger
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RoomObservationSessionTest {
    @Test
    fun sealingWaitsForActiveRoomTransactionBeforeClosingDatabase() = runBlocking {
        System.loadLibrary("sqlcipher")
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.deleteDatabase(DatabaseSchema.DATABASE_NAME)
        val logger = NoOpLogger
        val session = AppDatabaseSession(
            resource = AppDatabaseSessionResource(DatabaseProvider(context, logger)),
            keySource = SessionKeySource { DATABASE_KEY.copyOf() },
            eventLogger = DatabaseSessionEventLogger(logger),
        )
        val transactionStarted = CompletableDeferred<Unit>()
        val releaseTransaction = CompletableDeferred<Unit>()

        try {
            assertNull(session.unlock())
            val transaction = async(Dispatchers.IO) {
                session.transaction {
                    entryCommandDao().insertStrict(EntryEntity(entryId = "lease-entry", title = "Committed"))
                    transactionStarted.complete(Unit)
                    releaseTransaction.await()
                }
            }
            transactionStarted.await()

            val sealing = async(Dispatchers.IO) { session.seal() }
            withTimeout(10_000L) {
                session.lockStateFlow.first { it == SecureSessionState.SEALED }
            }

            assertFalse(transaction.isCompleted)
            assertFalse(sealing.isCompleted)
            val rejectedRead = runCatching { session.query { entryQueryDao().getById("lease-entry") } }
            assertTrue(rejectedRead.exceptionOrNull() is SessionLockedException)

            releaseTransaction.complete(Unit)
            transaction.await()
            sealing.await()

            assertNull(session.unlock())
            assertEquals("Committed", session.query { entryQueryDao().getById("lease-entry")?.title })
        } finally {
            releaseTransaction.complete(Unit)
            session.seal()
            context.deleteDatabase(DatabaseSchema.DATABASE_NAME)
        }
    }

    @Test
    fun activeRoomCollectorsEmitEmptyOnDatabaseOnlySealAndResubscribeAfterReopen() = runBlocking {
        System.loadLibrary("sqlcipher")
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.deleteDatabase(DatabaseSchema.DATABASE_NAME)
        val logger = NoOpLogger
        val session = AppDatabaseSession(
            resource = AppDatabaseSessionResource(DatabaseProvider(context, logger)),
            keySource = SessionKeySource { DATABASE_KEY.copyOf() },
            eventLogger = DatabaseSessionEventLogger(logger),
        )
        val access = FixedAuthenticatedAccess()
        val authenticatedState = access.authenticationState.value
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val activities = ObservationProbe<EntryActivityEntity>()
        val links = ObservationProbe<EntryLinkEntity>()
        val revisions = ObservationProbe<EntryRevisionEntity>()
        val activityValues = Channel<List<EntryActivityEntity>>(Channel.UNLIMITED)
        val linkValues = Channel<List<EntryLinkEntity>>(Channel.UNLIMITED)
        val revisionValues = Channel<List<EntryRevisionEntity>>(Channel.UNLIMITED)
        val jobs = mutableListOf<Job>()

        try {
            assertNull(session.unlock())
            session.transaction {
                entryCommandDao().insertStrict(EntryEntity(entryId = "entry-a", title = "A"))
                entryCommandDao().insertStrict(EntryEntity(entryId = "entry-b", title = "B"))
                entryActivityCommandDao().insertIdempotent(
                    EntryActivityEntity("activity-1", "entry-a", ActivityType.CREATE, createdAt = 1L),
                )
                entryLinkCommandDao().insertStrict(
                    EntryLinkEntity(
                        linkId = "link-1",
                        sourceEntryId = "entry-a",
                        targetEntryId = "entry-b",
                        relationType = EntryRelationType.RELATED_TO,
                        createdAt = 1L,
                        updatedAt = 1L,
                    ),
                )
                entryRevisionCommandDao().insertStrict(
                    EntryRevisionEntity(
                        revisionId = "revision-1",
                        entryId = "entry-a",
                        version = 1,
                        entryContentCipher = byteArrayOf(1),
                        sensitiveFieldCipherSet = byteArrayOf(1),
                        changeType = "VALUE_CHANGED",
                        createdAt = 1L,
                    ),
                )
            }

            jobs += scope.launch {
                activityObservations(session, access, activities)
                    .collect(activityValues::send)
            }
            jobs += scope.launch {
                linkObservations(session, access, links)
                    .collect(linkValues::send)
            }
            jobs += scope.launch {
                revisionObservations(session, access, revisions)
                    .collect(revisionValues::send)
            }

            assertEquals(1, nextNonEmpty(activityValues).size)
            assertEquals(1, nextNonEmpty(linkValues).size)
            assertEquals(1, nextNonEmpty(revisionValues).size)
            assertEquals(1, activities.starts.get())
            assertEquals(1, links.starts.get())
            assertEquals(1, revisions.starts.get())

            session.seal()

            assertEquals(emptyList<EntryActivityEntity>(), nextEmpty(activityValues))
            assertEquals(emptyList<EntryLinkEntity>(), nextEmpty(linkValues))
            assertEquals(emptyList<EntryRevisionEntity>(), nextEmpty(revisionValues))
            assertEquals(1, activities.completions.get())
            assertEquals(1, links.completions.get())
            assertEquals(1, revisions.completions.get())
            assertEquals(authenticatedState, access.authenticationState.value)

            assertNull(session.unlock())

            assertEquals(1, nextNonEmpty(activityValues).size)
            assertEquals(1, nextNonEmpty(linkValues).size)
            assertEquals(1, nextNonEmpty(revisionValues).size)
            assertEquals(2, activities.starts.get())
            assertEquals(2, links.starts.get())
            assertEquals(2, revisions.starts.get())
            assertEquals(authenticatedState, access.authenticationState.value)
        } finally {
            jobs.forEach(Job::cancel)
            scope.cancel()
            session.seal()
            context.deleteDatabase(DatabaseSchema.DATABASE_NAME)
        }
    }

    private fun <T> gatedObservations(
        session: AppDatabaseSession,
        access: SecureSessionAccessState,
        probe: ObservationProbe<T>,
        query: suspend com.aozijx.passly.data.local.database.AppDatabase.() -> Flow<List<T>>,
    ): Flow<List<T>> = session.observeWhenReadable(
        authorized = access.isAuthorized,
        emptyValue = { emptyList() },
    ) {
        query()
            .onStart { probe.starts.incrementAndGet() }
            .onCompletion { probe.completions.incrementAndGet() }
    }

    private fun activityObservations(
        session: AppDatabaseSession,
        access: SecureSessionAccessState,
        probe: ObservationProbe<EntryActivityEntity>,
    ) = gatedObservations(session, access, probe) { entryActivityQueryDao().observeAll() }

    private fun linkObservations(
        session: AppDatabaseSession,
        access: SecureSessionAccessState,
        probe: ObservationProbe<EntryLinkEntity>,
    ) = gatedObservations(session, access, probe) { entryLinkQueryDao().observeAll() }

    private fun revisionObservations(
        session: AppDatabaseSession,
        access: SecureSessionAccessState,
        probe: ObservationProbe<EntryRevisionEntity>,
    ) = gatedObservations(session, access, probe) {
        entryRevisionQueryDao().observeByEntryId("entry-a")
    }

    private suspend fun <T> nextNonEmpty(channel: Channel<List<T>>): List<T> =
        withTimeout(10_000L) {
            while (true) {
                val value = channel.receive()
                if (value.isNotEmpty()) return@withTimeout value
            }
            error("Unreachable")
        }

    private suspend fun <T> nextEmpty(channel: Channel<List<T>>): List<T> =
        withTimeout(10_000L) {
            while (true) {
                val value = channel.receive()
                if (value.isEmpty()) return@withTimeout value
            }
            error("Unreachable")
        }

    private class ObservationProbe<T> {
        val starts = AtomicInteger()
        val completions = AtomicInteger()
    }

    private class FixedAuthenticatedAccess : SecureSessionAccessState {
        override val authenticationState =
            kotlinx.coroutines.flow.MutableStateFlow<AuthenticationState>(
                AuthenticationState.Authenticated(authenticatedAtMs = 1L),
            )

        override fun isUnlocked() = true
    }

    private object NoOpLogger : Logger {
        override fun debug(event: EventKey, fields: LogFields) = Unit
        override fun info(event: EventKey, fields: LogFields) = Unit
        override fun degraded(event: EventKey, errorCode: ErrorCode, reason: ReasonCode, fields: LogFields) = Unit
        override fun warn(event: EventKey, errorCode: ErrorCode, reason: ReasonCode, throwable: Throwable, fields: LogFields) = Unit
        override fun error(event: EventKey, errorCode: ErrorCode, reason: ReasonCode, throwable: Throwable, fields: LogFields) = Unit
        override fun fatal(event: EventKey, errorCode: ErrorCode, reason: ReasonCode, throwable: Throwable, fields: LogFields) = Unit

        override suspend fun <T> withRootTrace(
            operation: OperationCode,
            block: suspend TraceScope.() -> T,
        ): T = TestTraceScope.block()

        override suspend fun <T> inSpan(
            operation: OperationCode,
            block: suspend TraceScope.() -> T,
        ): T = TestTraceScope.block()

        override fun captureTraceToken(singleUse: Boolean): TraceToken = error("Unused")

        override suspend fun <T> resumeTrace(
            token: TraceToken,
            operation: OperationCode,
            block: suspend TraceScope.() -> T,
        ): TraceResumeResult<T> = error("Unused")
    }

    private object TestTraceScope : TraceScope {
        override val traceId = TraceId("room_observation_trace")
        override val spanId = SpanId("room_observation_span")
        override val parentSpanId: SpanId? = null
    }

    private companion object {
        val DATABASE_KEY = "Room observation session test database key".toByteArray()
    }
}
