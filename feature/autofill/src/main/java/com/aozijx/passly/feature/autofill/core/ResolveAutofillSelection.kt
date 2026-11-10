package com.aozijx.passly.feature.autofill.core

import com.aozijx.passly.domain.access.model.AuthenticationPurpose
import com.aozijx.passly.domain.access.model.AuthenticationRequest
import com.aozijx.passly.domain.access.model.AuthenticationResult
import com.aozijx.passly.domain.access.model.FreshAuthenticationRequiredException
import com.aozijx.passly.domain.access.port.AuthenticationRequester
import com.aozijx.passly.domain.access.port.SecureSessionAccessState
import com.aozijx.passly.domain.autofill.model.AutofillCredentialResolution
import com.aozijx.passly.domain.autofill.model.AutofillCredentialSelection
import com.aozijx.passly.domain.autofill.model.AutofillGrantContext
import com.aozijx.passly.domain.autofill.model.AutofillInteractionId
import com.aozijx.passly.domain.autofill.model.AutofillInteractionRejection
import com.aozijx.passly.domain.autofill.model.AutofillInteractionState
import com.aozijx.passly.domain.autofill.model.AutofillWebConsent
import com.aozijx.passly.domain.autofill.model.AutofillInteractionUpdate
import com.aozijx.passly.domain.autofill.model.AutofillOperationFailure
import com.aozijx.passly.domain.autofill.model.AutofillSelectionOutcome
import com.aozijx.passly.domain.autofill.port.AutofillCredentialResolver
import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.feature.autofill.runtime.AutofillInteractionStore
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException

@Singleton
internal class ResolveAutofillSelection
    @Inject
    constructor(
        private val interactionStore: AutofillInteractionStore,
        private val credentialResolver: AutofillCredentialResolver,
        private val authenticationRequester: AuthenticationRequester,
        private val secureSessionAccessState: SecureSessionAccessState,
    ) {
        suspend operator fun invoke(
            interactionId: AutofillInteractionId,
            scope: AutofillGrantContext,
            entryId: EntryId,
            includeOtp: Boolean,
        ): AutofillSelectionOutcome {
            val snapshot =
                interactionStore.snapshot(interactionId)
                    ?: return AutofillSelectionOutcome.Rejected(
                        AutofillInteractionRejection.INTERACTION_NOT_FOUND,
                    )
            if (snapshot.scope != scope.normalized()) return AutofillSelectionOutcome.ScopeMismatch
            when (snapshot.state) {
                AutofillInteractionState.RequiresUnlock,
                AutofillInteractionState.Authenticating,
                -> return AutofillSelectionOutcome.Unauthorized

                AutofillInteractionState.Cancelled -> return AutofillSelectionOutcome.Cancelled

                else -> Unit
            }

            val sessionGeneration = secureSessionAccessState.autofillSessionGeneration()
            if (snapshot.source == com.aozijx.passly.domain.autofill.model.AutofillInteractionSource.LEGACY &&
                snapshot.scope.webDomain != null
            ) {
                if ((snapshot.webConsent as? AutofillWebConsent.Confirmed)?.sessionAtMs != sessionGeneration) {
                    return AutofillSelectionOutcome.Rejected(AutofillInteractionRejection.WEB_CONFIRMATION_EXPIRED)
                }
                if ((snapshot.webConsent as? AutofillWebConsent.EntryConfirmed)?.entryId != entryId) {
                    return AutofillSelectionOutcome.Rejected(AutofillInteractionRejection.WEB_ENTRY_NOT_CONFIRMED)
                }
            }
            return when (val selection = interactionStore.beginSelection(interactionId, entryId, sessionGeneration)) {
                is AutofillInteractionUpdate.Rejected -> AutofillSelectionOutcome.Rejected(selection.reason)
                is AutofillInteractionUpdate.Updated -> resolve(interactionId, scope, entryId, includeOtp)
            }
        }

        private suspend fun resolve(
            interactionId: AutofillInteractionId,
            scope: AutofillGrantContext,
            entryId: EntryId,
            includeOtp: Boolean,
        ): AutofillSelectionOutcome =
            try {
                resolveCredential(entryId, includeOtp)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: FreshAuthenticationRequiredException) {
                val snapshot = interactionStore.snapshot(interactionId)
                if (snapshot?.source == com.aozijx.passly.domain.autofill.model.AutofillInteractionSource.LEGACY &&
                    snapshot.scope.webDomain != null
                ) {
                    interactionStore.finish(interactionId, AutofillInteractionState.Cancelled)
                    return AutofillSelectionOutcome.Unauthorized
                }
                reauthenticateAndResolve(interactionId, scope, entryId, includeOtp)
            } catch (_: Exception) {
                AutofillSelectionOutcome.Failed(AutofillOperationFailure.CREDENTIAL_RESOLUTION_FAILED)
            }

        private suspend fun reauthenticateAndResolve(
            interactionId: AutofillInteractionId,
            scope: AutofillGrantContext,
            entryId: EntryId,
            includeOtp: Boolean,
        ): AutofillSelectionOutcome =
            try {
                when (authenticationRequester.authenticate(AuthenticationRequest(AuthenticationPurpose.AUTOFILL))) {
                    is AuthenticationResult.Success -> {
                        validateActiveSelection(interactionId, scope, entryId)
                            ?: resolveCredential(entryId, includeOtp)
                    }

                    is AuthenticationResult.Cancelled -> {
                        AutofillSelectionOutcome.Cancelled
                    }

                    is AuthenticationResult.Failure -> {
                        AutofillSelectionOutcome.Unauthorized
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                AutofillSelectionOutcome.Failed(AutofillOperationFailure.CREDENTIAL_RESOLUTION_FAILED)
            }

        private suspend fun validateActiveSelection(
            interactionId: AutofillInteractionId,
            scope: AutofillGrantContext,
            entryId: EntryId,
        ): AutofillSelectionOutcome? {
            val snapshot =
                interactionStore.snapshot(interactionId)
                    ?: return AutofillSelectionOutcome.Rejected(
                        AutofillInteractionRejection.INTERACTION_NOT_FOUND,
                    )
            if (snapshot.scope != scope.normalized()) return AutofillSelectionOutcome.ScopeMismatch
            return when (val state = snapshot.state) {
                AutofillInteractionState.Cancelled -> {
                    AutofillSelectionOutcome.Cancelled
                }

                is AutofillInteractionState.ResolvingSelection -> {
                    if (state.entryId == entryId) {
                        null
                    } else {
                        AutofillSelectionOutcome.Rejected(AutofillInteractionRejection.INVALID_STATE)
                    }
                }

                else -> {
                    AutofillSelectionOutcome.Rejected(AutofillInteractionRejection.INVALID_STATE)
                }
            }
        }

        private suspend fun resolveCredential(
            entryId: EntryId,
            includeOtp: Boolean,
        ): AutofillSelectionOutcome =
            when (
                val resolution =
                    credentialResolver.resolve(
                        AutofillCredentialSelection(entryId, includeOtp),
                    )
            ) {
                is AutofillCredentialResolution.Resolved -> AutofillSelectionOutcome.Filled(resolution.payload)

                AutofillCredentialResolution.NotFound,
                AutofillCredentialResolution.NotFillable,
                -> AutofillSelectionOutcome.CandidateUnavailable
            }
    }
