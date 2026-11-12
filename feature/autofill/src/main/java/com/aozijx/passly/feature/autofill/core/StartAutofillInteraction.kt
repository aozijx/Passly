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
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext

internal class StartAutofillInteraction
    @Inject
    constructor(
        private val store: AutofillInteractionStore,
        private val unlockLease: AutofillUnlockLease,
        private val vaultAccessState: SecureSessionAccessState,
        private val interactionTimeout: AutofillInteractionTimeout,
        private val finishInteraction: FinishAutofillInteraction,
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
            FinishAutofillInteraction(store, unlockLease),
        )

        suspend operator fun invoke(
            source: AutofillInteractionSource,
            scope: AutofillGrantContext,
            requireAuthentication: Boolean,
        ): AutofillInteractionSnapshot {
            val normalizedScope = requireNotNull(scope.normalized()) { "Invalid Autofill interaction scope" }
            unlockLease.awaitReadyForInteraction()
            val hasFullAccess = vaultAccessState.hasFullSecureSessionAccess()
            val snapshot =
                store.create(
                    source = source,
                    scope = normalizedScope,
                    initialState =
                        if (hasFullAccess && !requireAuthentication) {
                            AutofillInteractionState.Authorized
                        } else {
                            AutofillInteractionState.RequiresUnlock
                        },
                )
            try {
                val authorized =
                    if (
                        !requireAuthentication &&
                        !hasFullAccess &&
                        vaultAccessState.hasAutofillSessionAccess() &&
                        unlockLease.borrowIfOwned(snapshot.id)
                    ) {
                        when (val update = store.authorizeFromExistingSession(snapshot.id)) {
                            is AutofillInteractionUpdate.Updated -> {
                                update.snapshot
                            }

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
            } catch (failure: Throwable) {
                withContext(NonCancellable) {
                    try {
                        try {
                            finishInteraction.cancel(snapshot.id)
                        } finally {
                            store.removeTerminal(snapshot.id)
                        }
                    } catch (cleanupFailure: Throwable) {
                        if (cleanupFailure is Error && failure !is Error) {
                            cleanupFailure.addSuppressed(failure)
                            throw cleanupFailure
                        }
                        failure.addSuppressed(cleanupFailure)
                    }
                }
                throw failure
            }
        }
    }
