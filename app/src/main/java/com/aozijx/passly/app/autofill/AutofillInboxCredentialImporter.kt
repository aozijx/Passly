package com.aozijx.passly.app.autofill

import com.aozijx.passly.domain.autofill.model.AutofillInboxPendingDraft
import com.aozijx.passly.domain.autofill.model.AutofillSaveOutcome
import com.aozijx.passly.domain.autofill.port.AutofillInboxDraftImporter
import com.aozijx.passly.domain.autofill.port.AutofillInboxImportOutcome
import com.aozijx.passly.feature.autofill.core.SaveAutofillCredential
import javax.inject.Inject

internal class AutofillInboxCredentialImporter @Inject constructor(
    private val saveAutofillCredential: SaveAutofillCredential,
) : AutofillInboxDraftImporter {
    override suspend fun import(draft: AutofillInboxPendingDraft): AutofillInboxImportOutcome =
        when (saveAutofillCredential(draft.draft, draft.draftId)) {
            is AutofillSaveOutcome.Saved -> AutofillInboxImportOutcome.Imported
            AutofillSaveOutcome.InvalidDraft -> AutofillInboxImportOutcome.Invalid
            is AutofillSaveOutcome.Failed -> AutofillInboxImportOutcome.Retry
        }
}
