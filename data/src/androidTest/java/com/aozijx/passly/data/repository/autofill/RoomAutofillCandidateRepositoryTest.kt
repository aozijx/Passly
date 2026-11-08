package com.aozijx.passly.data.repository.autofill

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
import com.aozijx.passly.core.platform.VaultResourcePaths
import com.aozijx.passly.data.local.database.DatabaseProvider
import com.aozijx.passly.data.local.database.DatabaseSchema
import com.aozijx.passly.data.local.database.entity.EntryEntity
import com.aozijx.passly.data.local.database.session.AppDatabaseSession
import com.aozijx.passly.data.local.database.session.AppDatabaseSessionResource
import com.aozijx.passly.data.local.database.session.DatabaseSessionEventLogger
import com.aozijx.passly.data.testing.IsolatedTestContext
import com.aozijx.passly.domain.access.model.AuthenticationState
import com.aozijx.passly.domain.access.port.SecureSessionAccessState
import com.aozijx.passly.domain.autofill.model.AutofillCandidateSummary
import com.aozijx.passly.domain.autofill.model.AutofillCandidateQuery
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.model.query.MatchType
import com.aozijx.passly.runtime.session.SessionKeySource
import javax.inject.Provider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RoomAutofillCandidateRepositoryTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private lateinit var context: Context
    private lateinit var session: AppDatabaseSession
    private lateinit var repository: RoomAutofillCandidateRepository
    private val access = AutofillAuthenticatedAccess()

    @Before
    fun setUp() {
        System.loadLibrary("sqlcipher")
        context = IsolatedTestContext(ApplicationProvider.getApplicationContext(), temporaryFolder.root)
        context.deleteDatabase(DatabaseSchema.DATABASE_NAME)
        val logger = NoOpLogger
        session =
            AppDatabaseSession(
                resource = AppDatabaseSessionResource(DatabaseProvider(context, logger)),
                keySource = SessionKeySource { DATABASE_KEY.copyOf() },
                eventLogger = DatabaseSessionEventLogger(logger),
                accessState = Provider { access },
            )
        check(runBlocking { session.unlock() } == null)
        repository = RoomAutofillCandidateRepository(session)
    }

    @After
    fun tearDown() {
        runBlocking { session.seal() }
        VaultResourcePaths.resourceDirectories(context).forEach { it.deleteRecursively() }
        context.deleteDatabase(DatabaseSchema.DATABASE_NAME)
    }

    @Test
    fun applicationSearchNormalizesScopeAndRanksFavoritesBeforeRecencyWithinRequestedLimit() =
        runBlocking {
            insert(entry("recent", applicationIds = setOf("com.example.app"), updatedAt = 30L))
            insert(entry("favorite-old", applicationIds = setOf("com.example.app"), favorite = true, updatedAt = 10L))
            insert(entry("favorite-new", applicationIds = setOf("com.example.app"), favorite = true, updatedAt = 20L))
            insert(entry("unrelated", applicationIds = setOf("com.other.app"), favorite = true, updatedAt = 100L))

            val candidates =
                repository.search(
                    AutofillCandidateQuery(
                        packageName = " COM.EXAMPLE.APP ",
                        webDomain = null,
                        limit = 2,
                    ),
                )
            val minimumLimit =
                repository.search(
                    AutofillCandidateQuery(
                        packageName = "com.example.app",
                        webDomain = null,
                        limit = 0,
                    ),
                )

            assertEquals(listOf("favorite-new", "favorite-old"), candidates.map { it.entry.id.value })
            assertEquals(listOf(MatchType.APPLICATION_ID, MatchType.APPLICATION_ID), candidates.map { it.match.type })
            assertEquals(listOf("com.example.app", "com.example.app"), candidates.map { it.match.applicationId })
            assertEquals(listOf("favorite-new"), minimumLimit.map { it.entry.id.value })
        }

    @Test
    fun domainSearchAppliesGlobalMaximumAfterFavoriteAndRecencyOrdering() =
        runBlocking {
            repeat(12) { index ->
                insert(
                    entry(
                        id = "domain-$index",
                        domains = setOf("example.com"),
                        favorite = index == 0 || index == 1,
                        updatedAt = index.toLong(),
                    ),
                )
            }
            insert(entry("deleted", domains = setOf("example.com"), deletedAt = 50L))
            insert(entry("not-login", type = EntryType.NOTE, domains = setOf("example.com")))

            val candidates =
                repository.search(
                    AutofillCandidateQuery(
                        packageName = null,
                        webDomain = "https://example.com/path",
                        limit = Int.MAX_VALUE,
                    ),
                )

            assertEquals(10, candidates.size)
            assertEquals(
                listOf(
                    "domain-1",
                    "domain-0",
                    "domain-11",
                    "domain-10",
                    "domain-9",
                    "domain-8",
                    "domain-7",
                    "domain-6",
                    "domain-5",
                    "domain-4",
                ),
                candidates.map { it.entry.id.value },
            )
            assertEquals(List(10) { MatchType.WEB_DOMAIN }, candidates.map { it.match.type })
        }

    @Test
    fun searchReturnsNoCandidatesAfterAutofillSessionAccessIsRevoked() =
        runBlocking {
            insert(entry("candidate", applicationIds = setOf("com.example.app")))
            access.authenticationState.value = AuthenticationState.Locked

            val candidates =
                repository.search(
                    AutofillCandidateQuery(
                        packageName = "com.example.app",
                        webDomain = null,
                    ),
                )

            assertEquals(emptyList<AutofillCandidateSummary>(), candidates)
        }

    private suspend fun insert(entity: EntryEntity) {
        session.transactionForMaintenance { entryCommandDao().insertStrict(entity) }
    }

    private fun entry(
        id: String,
        type: EntryType = EntryType.LOGIN,
        applicationIds: Set<String> = emptySet(),
        domains: Set<String> = emptySet(),
        favorite: Boolean = false,
        updatedAt: Long = 1L,
        deletedAt: Long? = null,
    ) =
        EntryEntity(
            entryId = id,
            entryType = type,
            title = id,
            domains = domains,
            applicationIds = applicationIds,
            favorite = favorite,
            createdAt = 1L,
            updatedAt = updatedAt,
            deletedAt = deletedAt,
        )

    private class AutofillAuthenticatedAccess : SecureSessionAccessState {
        override val authenticationState =
            MutableStateFlow<AuthenticationState>(AuthenticationState.AutofillAuthenticated(1L))

        override fun isUnlocked() = true
    }

    private object NoOpLogger : Logger {
        override fun debug(event: EventKey, fields: LogFields) = Unit
        override fun info(event: EventKey, fields: LogFields) = Unit
        override fun degraded(event: EventKey, errorCode: ErrorCode, reason: ReasonCode, fields: LogFields) = Unit
        override fun warn(
            event: EventKey,
            errorCode: ErrorCode,
            reason: ReasonCode,
            throwable: Throwable,
            fields: LogFields,
        ) = Unit

        override fun error(
            event: EventKey,
            errorCode: ErrorCode,
            reason: ReasonCode,
            throwable: Throwable,
            fields: LogFields,
        ) = Unit

        override fun fatal(
            event: EventKey,
            errorCode: ErrorCode,
            reason: ReasonCode,
            throwable: Throwable,
            fields: LogFields,
        ) = Unit

        override suspend fun <T> withRootTrace(
            operation: OperationCode,
            block: suspend TraceScope.() -> T,
        ) = TestTraceScope.block()

        override suspend fun <T> inSpan(
            operation: OperationCode,
            block: suspend TraceScope.() -> T,
        ) = TestTraceScope.block()

        override fun captureTraceToken(singleUse: Boolean): TraceToken = error("Unused")

        override suspend fun <T> resumeTrace(
            token: TraceToken,
            operation: OperationCode,
            block: suspend TraceScope.() -> T,
        ): TraceResumeResult<T> = error("Unused")
    }

    private object TestTraceScope : TraceScope {
        override val traceId = TraceId("autofill_candidate")
        override val spanId = SpanId("autofill_candidate")
        override val parentSpanId: SpanId? = null
    }

    private companion object {
        val DATABASE_KEY = "Autofill candidate repository test key".toByteArray()
    }
}
