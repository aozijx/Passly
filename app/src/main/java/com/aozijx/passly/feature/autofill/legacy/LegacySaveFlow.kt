package com.aozijx.passly.feature.autofill.legacy

import com.aozijx.passly.domain.autofill.model.AutofillInboxEnqueueOutcome
import com.aozijx.passly.domain.autofill.model.AutofillInboxSource
import com.aozijx.passly.domain.autofill.model.AutofillRequest
import com.aozijx.passly.domain.autofill.model.AutofillSaveDraft
import com.aozijx.passly.domain.autofill.model.AutofillSaveOutcome
import com.aozijx.passly.domain.autofill.port.AutofillInboxQueue
import com.aozijx.passly.domain.settings.port.InteractionSettingsSource
import com.aozijx.passly.feature.autofill.core.AutofillSaveAccessPolicy
import com.aozijx.passly.feature.autofill.core.AutofillSaveDraftExtraction
import com.aozijx.passly.feature.autofill.core.AutofillSaveDraftExtractor
import com.aozijx.passly.feature.autofill.core.SaveAutofillCredential
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first

sealed interface LegacySavePreparation {
    data object Saved : LegacySavePreparation
    data object Queued : LegacySavePreparation
    data object Disabled : LegacySavePreparation
    data object InvalidDraft : LegacySavePreparation
    data object Failed : LegacySavePreparation
}

@Singleton
class LegacySaveFlow internal constructor(
    private val settingsSource: InteractionSettingsSource,
    private val canPersistImmediately: () -> Boolean,
    private val enqueueInbox: suspend (AutofillInboxSource, AutofillSaveDraft) -> AutofillInboxEnqueueOutcome,
    private val extractDraft: (AutofillRequest) -> AutofillSaveDraftExtraction,
    private val saveCredential: suspend (AutofillSaveDraft) -> AutofillSaveOutcome,
) {
    @Inject
    constructor(
        settingsSource: InteractionSettingsSource,
        saveAccessPolicy: AutofillSaveAccessPolicy,
        inboxQueue: AutofillInboxQueue,
        extractor: AutofillSaveDraftExtractor,
        saveCredential: SaveAutofillCredential,
    ) : this(
        settingsSource = settingsSource,
        canPersistImmediately = saveAccessPolicy::canPersistImmediately,
        enqueueInbox = inboxQueue::enqueue,
        extractDraft = extractor::invoke,
        saveCredential = saveCredential::invoke,
    )

    suspend fun prepare(request: AutofillRequest): LegacySavePreparation {
        if (!legacySaveEnabled()) return LegacySavePreparation.Disabled
        val draft = when (val extraction = extractDraft(request)) {
            is AutofillSaveDraftExtraction.Ready -> extraction.draft
            is AutofillSaveDraftExtraction.Rejected -> return LegacySavePreparation.InvalidDraft
        }
        if (canPersistImmediately()) return persistImmediately(draft)

        return when (enqueueInbox(AutofillInboxSource.LEGACY_SERVICE, draft)) {
            AutofillInboxEnqueueOutcome.Queued -> LegacySavePreparation.Queued
            AutofillInboxEnqueueOutcome.NotProvisioned,
            AutofillInboxEnqueueOutcome.Full,
            AutofillInboxEnqueueOutcome.Failed,
            -> LegacySavePreparation.Failed
        }
    }

    private suspend fun legacySaveEnabled(): Boolean =
        settingsSource.interaction.first().autofill.let { it.enabled && it.savePromptsEnabled }

    private suspend fun persistImmediately(draft: AutofillSaveDraft): LegacySavePreparation =
        when (saveCredential(draft)) {
            is AutofillSaveOutcome.Saved -> LegacySavePreparation.Saved
            AutofillSaveOutcome.InvalidDraft -> LegacySavePreparation.InvalidDraft
            is AutofillSaveOutcome.Failed -> LegacySavePreparation.Failed
        }
}
