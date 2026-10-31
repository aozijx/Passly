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
import com.aozijx.passly.domain.access.model.AuthenticationState
import com.aozijx.passly.domain.access.port.SecureSessionAccessState
import com.aozijx.passly.runtime.session.SecureSessionState
import com.aozijx.passly.runtime.session.SessionKeySource
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class AttachmentResourceGarbageCollectorTest {
    @Test
    fun unlockAutomaticallyResumesDurablePendingGarbageCollection() = runBlocking {
        System.loadLibrary("sqlcipher")
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.deleteDatabase(DatabaseSchema.DATABASE_NAME)
        val resourceId = "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb"
        val resourceFile = File(
            VaultResourcePaths.attachmentDir(context).apply(File::mkdirs),
            "content/$resourceId.enc",
        ).apply {
            parentFile?.mkdirs()
            writeText("encrypted")
        }
        val logger = NoOpLogger
        val session = AppDatabaseSession(
            resource = AppDatabaseSessionResource(DatabaseProvider(context, logger)),
            keySource = SessionKeySource { DATABASE_KEY.copyOf() },
            eventLogger = DatabaseSessionEventLogger(logger),
        )
        val access = AuthenticatedAccess(session)

        try {
            assertNull(session.unlock())
            session.transaction {
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
                while (resourceFile.exists() || session.query { attachmentResourceDao().getById(resourceId) } != null) {
                    delay(10L)
                }
            }
            assertFalse(resourceFile.exists())
            assertNull(session.query { attachmentResourceDao().getById(resourceId) })
        } finally {
            session.seal()
            resourceFile.delete()
            context.deleteDatabase(DatabaseSchema.DATABASE_NAME)
        }
    }

    private class AuthenticatedAccess(
        private val session: AppDatabaseSession,
    ) : SecureSessionAccessState {
        override val authenticationState = MutableStateFlow<AuthenticationState>(
            AuthenticationState.Authenticated(authenticatedAtMs = 1L),
        )

        override fun isUnlocked() = session.lockStateFlow.value == SecureSessionState.UNLOCKED
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
        override val traceId = TraceId("attachment_gc")
        override val spanId = SpanId("attachment_gc")
        override val parentSpanId: SpanId? = null
    }

    private companion object {
        val DATABASE_KEY = "Attachment GC restart test database key".toByteArray()
    }
}
