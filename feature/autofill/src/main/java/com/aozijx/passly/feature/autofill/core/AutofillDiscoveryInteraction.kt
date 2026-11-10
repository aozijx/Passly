package com.aozijx.passly.feature.autofill.core

import com.aozijx.passly.domain.autofill.model.AutofillDiscoveryOutcome
import com.aozijx.passly.domain.autofill.model.AutofillInteractionId

internal fun AutofillDiscoveryOutcome.interactionIdOrNull(): AutofillInteractionId? =
    when (this) {
        is AutofillDiscoveryOutcome.Candidates -> snapshot.id
        is AutofillDiscoveryOutcome.NoCandidates -> snapshot.id
        is AutofillDiscoveryOutcome.RequiresUnlock -> snapshot.id
        is AutofillDiscoveryOutcome.RequiresWebRecipientConfirmation -> snapshot.id
        is AutofillDiscoveryOutcome.ResumeSelection -> snapshot.id
        AutofillDiscoveryOutcome.Disabled,
        AutofillDiscoveryOutcome.UnsupportedFields,
        is AutofillDiscoveryOutcome.Failed,
        -> null
    }
