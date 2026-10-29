package com.aozijx.passly.feature.settings.security

import com.aozijx.passly.domain.access.model.AuthenticationMethod
import com.aozijx.passly.domain.access.model.AuthenticationResult
import com.aozijx.passly.domain.access.model.RecoveryCredentialCreation
import com.aozijx.passly.domain.access.model.RecoveryCredentialDraft
import com.aozijx.passly.domain.access.model.RecoveryCredentialFactory
import com.aozijx.passly.domain.access.model.RecoveryCredentialId
import com.aozijx.passly.domain.clipboard.port.SensitiveClipboardWriter
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class RecoveryCodeDraftInteractorTest {
    @Test
    fun `generation exposes an owned disclosure while interactor retains draft`() = runTest {
        val draft = FakeDraft("recovery-code")
        val interactor = RecoveryCodeDraftInteractor(
            draftFactory = RecoveryCredentialFactory { RecoveryCredentialCreation.Ready(draft) },
            clipboardWriter = RecordingClipboard(),
        )

        val result = interactor.generate() as RecoveryCodeDraftResult.Ready
        val revealed = result.disclosure.toCharArray()
        try {
            assertEquals("recovery-code", revealed.concatToString())
        } finally {
            revealed.fill('\u0000')
            result.disclosure.wipe()
            interactor.close()
        }

        assertTrue(draft.closed)
    }

    @Test
    fun `copy and dismiss never expose draft plaintext to presentation`() = runTest {
        val draft = FakeDraft("recovery-code")
        val clipboard = RecordingClipboard()
        val interactor = RecoveryCodeDraftInteractor(
            draftFactory = RecoveryCredentialFactory { RecoveryCredentialCreation.Ready(draft) },
            clipboardWriter = clipboard,
        )
        val result = interactor.generate() as RecoveryCodeDraftResult.Ready
        result.disclosure.wipe()

        assertSame(RecoveryCodeCopyResult.Copied, interactor.copy())
        assertEquals("recovery-code", clipboard.lastValue)

        interactor.dismiss()
        assertTrue(draft.closed)
        assertSame(RecoveryCodeCommitResult.NoActiveDraft, interactor.commit())
    }

    @Test
    fun `dismissed recovery draft cannot finish a pending clipboard write`() = runTest {
        val draft = FakeDraft("recovery-code")
        val clipboard = DelayedClipboard()
        val interactor = RecoveryCodeDraftInteractor(
            draftFactory = RecoveryCredentialFactory { RecoveryCredentialCreation.Ready(draft) },
            clipboardWriter = clipboard,
        )
        interactor.generate().let { (it as RecoveryCodeDraftResult.Ready).disclosure.wipe() }
        val pendingCopy = async { interactor.copy() }
        clipboard.writeStarted.await()

        interactor.dismiss()
        clipboard.finishWrite.complete(Unit)

        assertSame(RecoveryCodeCopyResult.NoActiveDraft, pendingCopy.await())
        assertNull(clipboard.lastValue)
        assertTrue(draft.closed)
    }

    private class FakeDraft(private val value: String) : RecoveryCredentialDraft {
        override val id = RecoveryCredentialId("draft-id")
        var closed = false

        override fun reveal(): CharArray? = if (closed) null else value.toCharArray()

        override suspend fun commit(): AuthenticationResult = AuthenticationResult.Success(
            method = AuthenticationMethod.RECOVERY_CODE,
            reusedSession = false,
        )

        override fun close() {
            closed = true
        }
    }

    private class RecordingClipboard : SensitiveClipboardWriter {
        var lastValue: String? = null
        override suspend fun writeSensitive(text: String) {
            lastValue = text
        }
    }

    private class DelayedClipboard : SensitiveClipboardWriter {
        val writeStarted = CompletableDeferred<Unit>()
        val finishWrite = CompletableDeferred<Unit>()
        var lastValue: String? = null

        override suspend fun writeSensitive(text: String) {
            writeStarted.complete(Unit)
            finishWrite.await()
            lastValue = text
        }

        override suspend fun writeSensitiveIf(text: String, canWrite: () -> Boolean): Boolean {
            writeStarted.complete(Unit)
            finishWrite.await()
            if (!canWrite()) return false
            lastValue = text
            return true
        }
    }
}
