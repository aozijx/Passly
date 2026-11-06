package com.aozijx.passly.feature.autofill.core.inbox

import com.aozijx.passly.domain.autofill.model.AutofillInboxPendingDraft
import com.aozijx.passly.domain.autofill.model.AutofillSaveOutcome
import com.aozijx.passly.domain.autofill.port.AutofillInboxDraftImporter
import com.aozijx.passly.domain.autofill.port.AutofillInboxImportOutcome
import com.aozijx.passly.feature.autofill.core.SaveAutofillCredential
import javax.inject.Inject

internal class AutofillInboxCredentialImporter
    @Inject
    constructor(
        private val saveAutofillCredential: SaveAutofillCredential,
    ) : AutofillInboxDraftImporter {
        override suspend fun import(draft: AutofillInboxPendingDraft): AutofillInboxImportOutcome =
            saveAutofillCredential(draft.draft, draft.draftId).toInboxImportOutcome()
    }

internal fun AutofillSaveOutcome.toInboxImportOutcome(): AutofillInboxImportOutcome =
    when (this) {
        is AutofillSaveOutcome.Saved -> AutofillInboxImportOutcome.Imported
        AutofillSaveOutcome.InvalidDraft -> AutofillInboxImportOutcome.Invalid
        is AutofillSaveOutcome.Failed -> AutofillInboxImportOutcome.Retry
    }
