package com.aozijx.passly.data.local.database.port

import android.content.Context
import android.content.ContextWrapper
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
import com.aozijx.passly.runtime.session.DatabaseSessionLifecycle
import com.aozijx.passly.runtime.session.SecureSessionState
import java.io.File
import java.nio.file.Files
import kotlinx.coroutines.async
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class VaultDatabaseResetControllerTest {
    @Test
    fun reopenFailureIsReturnedAfterFilesAreDeletedAndSessionRemainsSealed() = runTest {
        val root = Files.createTempDirectory("vault-reset-test").toFile()
        val actions = mutableListOf<String>()
        val reopenFailure = IllegalStateException("database reopen failed")
        val session = RecordingDatabaseSession(actions, unlockFailure = reopenFailure)
        val context = object : ContextWrapper(null) {
            override fun getDatabasePath(name: String) = File(root, name)
            override fun getFilesDir() = root
            override fun deleteDatabase(name: String): Boolean {
                actions += "delete"
                return true
            }
        }
        val controller = VaultDatabaseResetController(context, session, NoOpLogger)

        val result = controller.reset()

        assertEquals(reopenFailure, result)
        assertEquals(listOf("seal-start", "seal-finished", "delete", "unlock"), actions)
        assertEquals(SecureSessionState.SEALED, session.lockState)
    }

    @Test
    fun cancellationDuringSealFinishesResetAndReopensBeforePropagating() = runTest {
        val root = Files.createTempDirectory("vault-reset-test").toFile()
        val actions = mutableListOf<String>()
        val sealStarted = CompletableDeferred<Unit>()
        val finishSeal = CompletableDeferred<Unit>()
        val session = RecordingDatabaseSession(actions, sealStarted, finishSeal)
        val context = object : ContextWrapper(null) {
            override fun getDatabasePath(name: String) = File(root, name)
            override fun getFilesDir() = root
            override fun deleteDatabase(name: String): Boolean {
                actions += "delete"
                return true
            }
        }
        val controller = VaultDatabaseResetController(context, session, NoOpLogger)

        val reset = async { controller.reset() }
        sealStarted.await()
        reset.cancel()
        finishSeal.complete(Unit)
        try {
            reset.await()
            throw AssertionError("cancellation must remain visible to the caller")
        } catch (_: CancellationException) {
            // Expected after the destructive transition has reached a coherent end state.
        }

        assertEquals(listOf("seal-start", "seal-finished", "delete", "unlock"), actions)
    }

    private class RecordingDatabaseSession(
        private val actions: MutableList<String>,
        private val sealStarted: CompletableDeferred<Unit> = CompletableDeferred(),
        private val finishSeal: CompletableDeferred<Unit> = CompletableDeferred(Unit),
        private val unlockFailure: Throwable? = null,
    ) : DatabaseSessionLifecycle {
        override var lockState: SecureSessionState = SecureSessionState.UNLOCKED

        override suspend fun seal() {
            actions += "seal-start"
            sealStarted.complete(Unit)
            finishSeal.await()
            actions += "seal-finished"
            lockState = SecureSessionState.SEALED
        }

        override suspend fun unlock(): Throwable? {
            actions += "unlock"
            if (unlockFailure == null) lockState = SecureSessionState.UNLOCKED
            return unlockFailure
        }

        override suspend fun softLock() = Unit
    }

    private object NoOpLogger : Logger {
        private val trace = object : TraceScope {
            override val traceId = TraceId("0123456789abcdef")
            override val spanId = SpanId("0123456789abcdef")
            override val parentSpanId: SpanId? = null
        }

        override fun debug(event: EventKey, fields: LogFields) = Unit
        override fun info(event: EventKey, fields: LogFields) = Unit
        override fun degraded(event: EventKey, errorCode: ErrorCode, reason: ReasonCode, fields: LogFields) = Unit
        override fun warn(event: EventKey, errorCode: ErrorCode, reason: ReasonCode, throwable: Throwable, fields: LogFields) = Unit
        override fun error(event: EventKey, errorCode: ErrorCode, reason: ReasonCode, throwable: Throwable, fields: LogFields) = Unit
        override fun fatal(event: EventKey, errorCode: ErrorCode, reason: ReasonCode, throwable: Throwable, fields: LogFields) = Unit
        override suspend fun <T> withRootTrace(operation: OperationCode, block: suspend TraceScope.() -> T) = block(trace)
        override suspend fun <T> inSpan(operation: OperationCode, block: suspend TraceScope.() -> T) = block(trace)
        override fun captureTraceToken(singleUse: Boolean) = TraceToken("0123456789abcdef")
        override suspend fun <T> resumeTrace(
            token: TraceToken,
            operation: OperationCode,
            block: suspend TraceScope.() -> T,
        ) = TraceResumeResult.Unavailable
    }
}
