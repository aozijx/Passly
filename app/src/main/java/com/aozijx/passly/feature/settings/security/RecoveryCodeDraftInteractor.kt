package com.aozijx.passly.feature.settings.security

import com.aozijx.passly.domain.access.model.AuthenticationResult
import com.aozijx.passly.domain.access.model.RecoveryCredentialCreation
import com.aozijx.passly.domain.access.model.RecoveryCredentialDraft
import com.aozijx.passly.domain.access.model.RecoveryCredentialFactory
import com.aozijx.passly.domain.clipboard.port.SensitiveClipboardWriter
import com.aozijx.passly.domain.sensitive.OwnedChars
import com.aozijx.passly.domain.sensitive.SensitiveValue
import javax.inject.Inject

sealed interface RecoveryCodeDraftResult {
    data class Ready(val disclosure: SensitiveValue) : RecoveryCodeDraftResult
    data object Cancelled : RecoveryCodeDraftResult
    data object Failed : RecoveryCodeDraftResult
}

sealed interface RecoveryCodeCopyResult {
    data object Copied : RecoveryCodeCopyResult
    data object NoActiveDraft : RecoveryCodeCopyResult
}

sealed interface RecoveryCodeCommitResult {
    data object Committed : RecoveryCodeCommitResult
    data object Failed : RecoveryCodeCommitResult
    data object NoActiveDraft : RecoveryCodeCommitResult
}

class RecoveryCodeDraftInteractor @Inject constructor(
    private val draftFactory: RecoveryCredentialFactory,
    private val clipboardWriter: SensitiveClipboardWriter,
) : AutoCloseable {
    private var activeDraft: RecoveryCredentialDraft? = null

    suspend fun generate(): RecoveryCodeDraftResult {
        closeDraft()
        return when (val creation = draftFactory.create()) {
            is RecoveryCredentialCreation.Ready -> prepare(creation.draft)
            RecoveryCredentialCreation.Cancelled -> RecoveryCodeDraftResult.Cancelled
            is RecoveryCredentialCreation.Failed -> RecoveryCodeDraftResult.Failed
        }
    }

    suspend fun copy(): RecoveryCodeCopyResult {
        val draft = activeDraft ?: return RecoveryCodeCopyResult.NoActiveDraft
        val chars = draft.reveal() ?: return RecoveryCodeCopyResult.NoActiveDraft
        return try {
            if (clipboardWriter.writeSensitiveIf(String(chars)) { activeDraft === draft }) {
                RecoveryCodeCopyResult.Copied
            } else {
                RecoveryCodeCopyResult.NoActiveDraft
            }
        } finally {
            chars.fill('\u0000')
        }
    }

    suspend fun commit(): RecoveryCodeCommitResult {
        val draft = activeDraft ?: return RecoveryCodeCommitResult.NoActiveDraft
        return if (draft.commit() is AuthenticationResult.Success) {
            activeDraft = null
            RecoveryCodeCommitResult.Committed
        } else {
            RecoveryCodeCommitResult.Failed
        }
    }

    fun dismiss() = closeDraft()

    override fun close() = closeDraft()

    private fun prepare(draft: RecoveryCredentialDraft): RecoveryCodeDraftResult {
        val chars = draft.reveal()
        if (chars == null) {
            draft.close()
            return RecoveryCodeDraftResult.Failed
        }
        activeDraft = draft
        return RecoveryCodeDraftResult.Ready(OwnedChars.take(chars))
    }

    private fun closeDraft() {
        activeDraft?.close()
        activeDraft = null
    }
}
