package com.aozijx.passly.feature.autofill.core

import com.aozijx.passly.domain.access.port.SecureSessionAccessState
import com.aozijx.passly.domain.autofill.model.AutofillGrantContext
import com.aozijx.passly.domain.autofill.model.AutofillInteractionSnapshot
import com.aozijx.passly.domain.autofill.model.AutofillInteractionSource
import com.aozijx.passly.domain.autofill.model.AutofillInteractionState
import com.aozijx.passly.domain.autofill.model.AutofillInteractionUpdate
import com.aozijx.passly.feature.autofill.runtime.AutofillInteractionStore
import com.aozijx.passly.feature.autofill.runtime.AutofillInteractionTimeout
import com.aozijx.passly.feature.autofill.runtime.AutofillUnlockLease
import javax.inject.Inject

class StartAutofillInteraction @Inject constructor(
    private val store: AutofillInteractionStore,
    private val unlockLease: AutofillUnlockLease,
    private val vaultAccessState: SecureSessionAccessState,
    private val interactionTimeout: AutofillInteractionTimeout,
) {
    internal constructor(
        store: AutofillInteractionStore,
        unlockLease: AutofillUnlockLease,
        vaultAccessState: SecureSessionAccessState,
    ) : this(
        store,
        unlockLease,
        vaultAccessState,
        AutofillInteractionTimeout.disabled(store, unlockLease),
    )

    suspend operator fun invoke(
        source: AutofillInteractionSource,
        scope: AutofillGrantContext,
        requireAuthentication: Boolean,
    ): AutofillInteractionSnapshot {
        val hasFullAccess = vaultAccessState.hasFullSecureSessionAccess()
        val snapshot = store.create(
            source = source,
            scope = scope,
            initialState = if (hasFullAccess && !requireAuthentication) {
                AutofillInteractionState.Authorized
            } else {
                AutofillInteractionState.RequiresUnlock
            },
        )
        val authorized = if (
            !requireAuthentication &&
            !hasFullAccess &&
            vaultAccessState.hasAutofillSessionAccess() &&
            unlockLease.borrowIfOwned(snapshot.id)
        ) {
            when (val update = store.authorizeFromExistingSession(snapshot.id)) {
                is AutofillInteractionUpdate.Updated -> update.snapshot
                is AutofillInteractionUpdate.Rejected -> {
                    unlockLease.release(snapshot.id)
                    snapshot
                }
            }
        } else {
            snapshot
        }
        interactionTimeout.schedule(snapshot.id)
        return authorized
    }
}
