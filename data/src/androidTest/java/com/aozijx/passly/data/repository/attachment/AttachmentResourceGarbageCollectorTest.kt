package com.aozijx.passly.data.repository.attachment

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
import com.aozijx.passly.data.local.database.entity.AttachmentResourceEntity
import com.aozijx.passly.data.local.database.entity.AttachmentResourceState
import com.aozijx.passly.data.local.database.session.AppDatabaseSession
import com.aozijx.passly.data.local.database.session.AppDatabaseSessionResource
import com.aozijx.passly.data.local.database.session.DatabaseSessionEventLogger
import com.aozijx.passly.data.testing.IsolatedTestContext
import com.aozijx.passly.domain.access.model.AuthenticationState
import com.aozijx.passly.domain.access.port.SecureSessionAccessState
import com.aozijx.passly.runtime.session.SecureSessionState
import com.aozijx.passly.runtime.session.SessionKeySource
import java.io.File
import javax.inject.Provider
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AttachmentResourceGarbageCollectorTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun unlockDrainsMoreThanOnePendingBatch() =
        runBlocking {
            System.loadLibrary("sqlcipher")
            val context = isolatedContext()
            context.deleteDatabase(DatabaseSchema.DATABASE_NAME)
            val logger = NoOpLogger
            val (session, access) = createSession(context, logger)
            val resources = (1..130).map { index -> createPendingResource(context, index) }

            try {
                assertNull(session.unlock())
                session.transactionForMaintenance {
                    resources.forEach { (id, file) ->
                        attachmentResourceDao().insertStrict(
                            AttachmentResourceEntity(
                                resourceId = id,
                                fileSize = file.length(),
                                lifecycleState = AttachmentResourceState.PENDING_GC,
                                createdAt = 1L,
                                enqueuedAt = 2L,
                            ),
                        )
                    }
                }
                session.seal()
                AttachmentResourceGarbageCollector(context, session, access, logger)
                assertNull(session.unlock())

                withTimeout(20_000L) {
                    while (
                        resources.any { (_, file) -> file.exists() } ||
                        session.queryForMaintenance { attachmentResourceDao().getPendingGc(200).isNotEmpty() }
                    ) {
                        delay(10L)
                    }
                }

                assertTrue(resources.none { (_, file) -> file.exists() })
                assertTrue(session.queryForMaintenance { attachmentResourceDao().getPendingGc(200).isEmpty() })
            } finally {
                session.seal()
                resources.forEach { (_, file) -> file.deleteRecursively() }
                context.deleteDatabase(DatabaseSchema.DATABASE_NAME)
            }
        }

    @Test
    fun failedFileDeletionRetriesWithoutAnotherSessionWakeup() =
        runBlocking {
            System.loadLibrary("sqlcipher")
            val context = isolatedContext()
            context.deleteDatabase(DatabaseSchema.DATABASE_NAME)
            val logger = NoOpLogger
            val (session, access) = createSession(context, logger)
            val (resourceId, resourceFile) = createPendingResource(context, 1001)
            val childFile = File(resourceFile, "still-present")

            try {
                assertNull(session.unlock())
                resourceFile.delete()
                resourceFile.mkdirs()
                childFile.writeText("prevent directory deletion")
                session.transactionForMaintenance {
                    attachmentResourceDao().insertStrict(
                        AttachmentResourceEntity(
                            resourceId = resourceId,
                            fileSize = 1L,
                            lifecycleState = AttachmentResourceState.PENDING_GC,
                            createdAt = 1L,
                            enqueuedAt = 2L,
                        ),
                    )
                }
                session.seal()
                AttachmentResourceGarbageCollector(context, session, access, logger)
                assertNull(session.unlock())

                withTimeout(10_000L) {
                    while (session.queryForMaintenance { attachmentResourceDao().getById(resourceId)?.attemptCount ?: 0 } == 0) {
                        delay(10L)
                    }
                }
                childFile.delete()

                withTimeout(10_000L) {
                    while (resourceFile.exists() || session.queryForMaintenance { attachmentResourceDao().getById(resourceId) != null }) {
                        delay(10L)
                    }
                }

                assertFalse(resourceFile.exists())
                assertNull(session.queryForMaintenance { attachmentResourceDao().getById(resourceId) })
            } finally {
                session.seal()
                resourceFile.deleteRecursively()
                context.deleteDatabase(DatabaseSchema.DATABASE_NAME)
            }
        }

    @Test
    fun unlockAutomaticallyResumesDurablePendingGarbageCollection() =
        runBlocking {
            System.loadLibrary("sqlcipher")
            val context = isolatedContext()
            context.deleteDatabase(DatabaseSchema.DATABASE_NAME)
            val resourceId = "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb"
            val resourceFile =
                File(
                    VaultResourcePaths.attachmentDir(context).apply(File::mkdirs),
                    "content/$resourceId.enc",
                ).apply {
                    parentFile?.mkdirs()
                    writeText("encrypted")
                }
            val logger = NoOpLogger
            val access = AuthenticatedAccess()
            val session =
                AppDatabaseSession(
                    resource = AppDatabaseSessionResource(DatabaseProvider(context, logger)),
                    keySource = SessionKeySource { DATABASE_KEY.copyOf() },
                    eventLogger = DatabaseSessionEventLogger(logger),
                    accessState = Provider { access },
                )
            access.bind(session)

            try {
                assertNull(session.unlock())
                session.transactionForMaintenance {
                    attachmentResourceDao().insertStrict(
                        AttachmentResourceEntity(
                            resourceId = resourceId,
                            fileSize = resourceFile.length(),
                            lifecycleState = AttachmentResourceState.PENDING_GC,
                            createdAt = 1L,
                            enqueuedAt = 2L,
                        ),
                    )
                }
                session.seal()

                AttachmentResourceGarbageCollector(context, session, access, logger)
                delay(100L)
                assertTrue(resourceFile.exists())
                assertNull(session.unlock())

                withTimeout(10_000L) {
                    while (resourceFile.exists() || session.queryForMaintenance { attachmentResourceDao().getById(resourceId) } != null) {
                        delay(10L)
                    }
                }
                assertFalse(resourceFile.exists())
                assertNull(session.queryForMaintenance { attachmentResourceDao().getById(resourceId) })
            } finally {
                session.seal()
                resourceFile.delete()
                context.deleteDatabase(DatabaseSchema.DATABASE_NAME)
            }
        }

    private class AuthenticatedAccess : SecureSessionAccessState {
        private var isDatabaseUnlocked: () -> Boolean = { true }

        override val authenticationState =
            MutableStateFlow<AuthenticationState>(
                AuthenticationState.Authenticated(authenticatedAtMs = 1L),
            )

        override fun isUnlocked() = isDatabaseUnlocked()

        fun bind(session: AppDatabaseSession) {
            isDatabaseUnlocked = { session.lockStateFlow.value == SecureSessionState.UNLOCKED }
        }
    }

    private fun createSession(
        context: Context,
        logger: Logger,
    ): Pair<AppDatabaseSession, AuthenticatedAccess> {
        val access = AuthenticatedAccess()
        val session =
            AppDatabaseSession(
                resource = AppDatabaseSessionResource(DatabaseProvider(context, logger)),
                keySource = SessionKeySource { DATABASE_KEY.copyOf() },
                eventLogger = DatabaseSessionEventLogger(logger),
                accessState = Provider { access },
            )
        access.bind(session)
        return session to access
    }

    private fun createPendingResource(
        context: Context,
        index: Int,
    ): Pair<String, File> {
        val resourceId = index.toString().padStart(64, '0')
        val resourceFile =
            File(
                VaultResourcePaths.attachmentDir(context).apply(File::mkdirs),
                "content/$resourceId.enc",
            ).apply {
                parentFile?.mkdirs()
                writeText("encrypted")
            }
        return resourceId to resourceFile
    }

    private object NoOpLogger : Logger {
        override fun debug(
            event: EventKey,
            fields: LogFields,
        ) = Unit

        override fun info(
            event: EventKey,
            fields: LogFields,
        ) = Unit

        override fun degraded(
            event: EventKey,
            errorCode: ErrorCode,
            reason: ReasonCode,
            fields: LogFields,
        ) = Unit

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
        override val traceId = TraceId("attachment_gc")
        override val spanId = SpanId("attachment_gc")
        override val parentSpanId: SpanId? = null
    }

    private companion object {
        val DATABASE_KEY = "Attachment GC restart test database key".toByteArray()
    }

    private fun isolatedContext(): Context =
        IsolatedTestContext(
            ApplicationProvider.getApplicationContext(),
            temporaryFolder.root,
        )
}
