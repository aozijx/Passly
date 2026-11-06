package com.aozijx.passly.feature.autofill.core.inbox

import com.aozijx.passly.domain.autofill.model.AutofillOperationFailure
import com.aozijx.passly.domain.autofill.model.AutofillSaveOutcome
import com.aozijx.passly.domain.autofill.port.AutofillInboxImportOutcome
import com.aozijx.passly.domain.entry.model.EntryId
import org.junit.Assert.assertEquals
import org.junit.Test

class AutofillInboxImportOutcomeMappingTest {
    @Test
    fun savedCredentialIsImported() {
        assertEquals(
            AutofillInboxImportOutcome.Imported,
            AutofillSaveOutcome.Saved(EntryId("entry-id")).toInboxImportOutcome(),
        )
    }

    @Test
    fun invalidDraftIsRejected() {
        assertEquals(
            AutofillInboxImportOutcome.Invalid,
            AutofillSaveOutcome.InvalidDraft.toInboxImportOutcome(),
        )
    }

    @Test
    fun failedPersistenceRemainsRetryable() {
        assertEquals(
            AutofillInboxImportOutcome.Retry,
            AutofillSaveOutcome.Failed(AutofillOperationFailure.PERSISTENCE_FAILED).toInboxImportOutcome(),
        )
    }
}
