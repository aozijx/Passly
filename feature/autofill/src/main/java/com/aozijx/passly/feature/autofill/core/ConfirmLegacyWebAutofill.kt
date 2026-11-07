package com.aozijx.passly.feature.autofill.core

import com.aozijx.passly.domain.access.port.SecureSessionAccessState
import com.aozijx.passly.domain.autofill.model.AutofillGrantContext
import com.aozijx.passly.domain.autofill.model.AutofillInteractionId
import com.aozijx.passly.domain.autofill.model.AutofillInteractionUpdate
import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.feature.autofill.runtime.AutofillInteractionStore
import javax.inject.Inject
import javax.inject.Singleton

/** Records the two explicit disclosures required by an unverified Legacy web Autofill request. */
@Singleton
internal class ConfirmLegacyWebAutofill
    @Inject
    constructor(
        private val interactionStore: AutofillInteractionStore,
        private val secureSessionAccessState: SecureSessionAccessState,
    ) {
        suspend fun confirmRecipient(
            interactionId: AutofillInteractionId,
            scope: AutofillGrantContext,
        ): Boolean =
            secureSessionAccessState.autofillSessionGeneration()?.let { sessionAtMs ->
                interactionStore.confirmWebRecipient(interactionId, scope, sessionAtMs) is AutofillInteractionUpdate.Updated
            } ?: false

        suspend fun confirmEntry(
            interactionId: AutofillInteractionId,
            scope: AutofillGrantContext,
            entryId: EntryId,
        ): Boolean =
            secureSessionAccessState.autofillSessionGeneration()?.let { sessionAtMs ->
                interactionStore.confirmWebEntry(interactionId, scope, entryId, sessionAtMs) is AutofillInteractionUpdate.Updated
            } ?: false
    }
