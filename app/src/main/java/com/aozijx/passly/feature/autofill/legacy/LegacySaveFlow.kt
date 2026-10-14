package com.aozijx.passly.feature.autofill.legacy

import com.aozijx.passly.domain.access.port.SecureSessionAccessState
import com.aozijx.passly.domain.autofill.model.AutofillAuthorizationOutcome
import com.aozijx.passly.domain.autofill.model.AutofillGrantContext
import com.aozijx.passly.domain.autofill.model.AutofillInteractionId
import com.aozijx.passly.domain.autofill.model.AutofillInteractionSource
import com.aozijx.passly.domain.autofill.model.AutofillRequest
import com.aozijx.passly.domain.autofill.model.AutofillSaveDraft
import com.aozijx.passly.domain.autofill.model.AutofillSaveOutcome
import com.aozijx.passly.domain.settings.port.InteractionSettingsSource
import com.aozijx.passly.feature.autofill.core.AuthorizeAutofillInteraction
import com.aozijx.passly.feature.autofill.core.AutofillSaveDraftExtraction
import com.aozijx.passly.feature.autofill.core.AutofillSaveDraftExtractor
import com.aozijx.passly.feature.autofill.core.FinishAutofillInteraction
import com.aozijx.passly.feature.autofill.core.SaveAutofillCredential
import com.aozijx.passly.feature.autofill.core.StartAutofillInteraction
import com.aozijx.passly.feature.autofill.legacy.runtime.LegacySaveDraftStore
import com.aozijx.passly.feature.autofill.legacy.runtime.LegacySaveToken
import com.aozijx.passly.feature.autofill.runtime.AutofillInteractionStore
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

sealed interface LegacySavePreparation {
    data object Saved : LegacySavePreparation
    data class Staged(val token: LegacySaveToken) : LegacySavePreparation
    data object Disabled : LegacySavePreparation
    data object InvalidDraft : LegacySavePreparation
    data object Failed : LegacySavePreparation
}

class LegacySaveContinuation internal constructor(
    val interactionId: AutofillInteractionId,
    internal val draft: AutofillSaveDraft,
    internal val scope: AutofillGrantContext,
)

sealed interface LegacySaveBeginResult {
    data class Ready(val continuation: LegacySaveContinuation) : LegacySaveBeginResult
    data object Disabled : LegacySaveBeginResult
    data object Unavailable : LegacySaveBeginResult
}

enum class LegacySaveCompletion {
    Saved,
    NotAuthorized,
    NotSaved,
}

@Singleton
class LegacySaveFlow internal constructor(
    private val settingsSource: InteractionSettingsSource,
    private val vaultAccessState: SecureSessionAccessState,
    private val draftStore: LegacySaveDraftStore,
    private val startInteraction: StartAutofillInteraction,
    private val authorizeInteraction: AuthorizeAutofillInteraction,
    private val finishInteraction: FinishAutofillInteraction,
    private val interactionStore: AutofillInteractionStore,
    private val extractDraft: (AutofillRequest) -> AutofillSaveDraftExtraction,
    private val saveCredential: suspend (AutofillSaveDraft) -> AutofillSaveOutcome,
) {
    @Inject
    constructor(
        settingsSource: InteractionSettingsSource,
        vaultAccessState: SecureSessionAccessState,
        draftStore: LegacySaveDraftStore,
        startInteraction: StartAutofillInteraction,
        authorizeInteraction: AuthorizeAutofillInteraction,
        finishInteraction: FinishAutofillInteraction,
        interactionStore: AutofillInteractionStore,
        extractor: AutofillSaveDraftExtractor,
        saveCredential: SaveAutofillCredential,
    ) : this(
        settingsSource,
        vaultAccessState,
        draftStore,
        startInteraction,
        authorizeInteraction,
        finishInteraction,
        interactionStore,
        extractor::invoke,
        saveCredential::invoke,
    )

    private val completionMutex = Mutex()
    private val completionsInProgress = mutableSetOf<AutofillInteractionId>()

    suspend fun prepare(request: AutofillRequest): LegacySavePreparation {
        if (!legacySaveEnabled()) return LegacySavePreparation.Disabled
        val draft = when (val extraction = extractDraft(request)) {
            is AutofillSaveDraftExtraction.Ready -> extraction.draft
            is AutofillSaveDraftExtraction.Rejected -> return LegacySavePreparation.InvalidDraft
        }
        if (vaultAccessState.hasFullSecureSessionAccess()) {
            return persistImmediately(draft)
        }
        val scope = AutofillGrantContext(draft.packageName, draft.webDomain).normalized()
        return LegacySavePreparation.Staged(draftStore.stage(draft, scope))
    }

    suspend fun beginContinuation(token: LegacySaveToken): LegacySaveBeginResult {
        val pending = draftStore.consume(token) ?: return LegacySaveBeginResult.Unavailable
        if (!legacySaveEnabled()) return LegacySaveBeginResult.Disabled
        val interaction = startInteraction(
            source = AutofillInteractionSource.LEGACY_SAVE,
            scope = pending.scope,
            requireAuthentication = false,
        )
        return LegacySaveBeginResult.Ready(
            LegacySaveContinuation(interaction.id, pending.draft, pending.scope),
        )
    }

    suspend fun completeContinuation(
        continuation: LegacySaveContinuation,
    ): LegacySaveCompletion {
        if (!claimCompletion(continuation.interactionId)) return LegacySaveCompletion.NotSaved
        var saved = false
        return try {
            val snapshot = interactionStore.snapshot(continuation.interactionId)
            if (
                snapshot == null ||
                snapshot.source != AutofillInteractionSource.LEGACY_SAVE ||
                snapshot.scope != continuation.scope
            ) {
                return LegacySaveCompletion.NotSaved
            }
            val authorization = authorizeInteraction(continuation.interactionId)
            if (authorization !is AutofillAuthorizationOutcome.Authorized) {
                return LegacySaveCompletion.NotAuthorized
            }
            when (saveCredential(continuation.draft)) {
                is AutofillSaveOutcome.Saved -> LegacySaveCompletion.Saved.also { saved = true }
                AutofillSaveOutcome.InvalidDraft,
                is AutofillSaveOutcome.Failed,
                -> LegacySaveCompletion.NotSaved
            }
        } finally {
            withContext(NonCancellable) {
                if (saved) {
                    finishInteraction.complete(continuation.interactionId)
                } else {
                    finishInteraction.cancel(continuation.interactionId)
                }
                releaseCompletion(continuation.interactionId)
            }
        }
    }

    suspend fun cancel(interactionId: AutofillInteractionId) {
        withContext(NonCancellable) { finishInteraction.cancel(interactionId) }
    }

    private suspend fun legacySaveEnabled(): Boolean =
        settingsSource.interaction.first().autofill.let { it.enabled && it.savePromptsEnabled }

    private suspend fun persistImmediately(draft: AutofillSaveDraft): LegacySavePreparation = try {
        when (saveCredential(draft)) {
            is AutofillSaveOutcome.Saved -> LegacySavePreparation.Saved
            AutofillSaveOutcome.InvalidDraft -> LegacySavePreparation.InvalidDraft
            is AutofillSaveOutcome.Failed -> LegacySavePreparation.Failed
        }
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (_: Exception) {
        LegacySavePreparation.Failed
    }

    private suspend fun claimCompletion(id: AutofillInteractionId): Boolean =
        completionMutex.withLock { completionsInProgress.add(id) }

    private suspend fun releaseCompletion(id: AutofillInteractionId) {
        completionMutex.withLock { completionsInProgress.remove(id) }
    }
}
