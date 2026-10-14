package com.aozijx.passly.feature.autofill.credential

import com.aozijx.passly.domain.autofill.model.AutofillAuthorizationOutcome
import com.aozijx.passly.domain.autofill.model.AutofillDiscoveryOutcome
import com.aozijx.passly.domain.autofill.model.AutofillGrantContext
import com.aozijx.passly.domain.autofill.model.AutofillInteractionId
import com.aozijx.passly.domain.autofill.model.AutofillInteractionSnapshot
import com.aozijx.passly.domain.autofill.model.AutofillInteractionSource
import com.aozijx.passly.domain.autofill.model.AutofillRequest
import com.aozijx.passly.domain.autofill.model.AutofillSaveDraft
import com.aozijx.passly.domain.autofill.model.AutofillSaveOutcome
import com.aozijx.passly.domain.autofill.model.AutofillSelectionOutcome
import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.settings.port.InteractionSettingsSource
import com.aozijx.passly.feature.autofill.core.AuthorizeAutofillInteraction
import com.aozijx.passly.feature.autofill.core.DiscoverAutofillCandidates
import com.aozijx.passly.feature.autofill.core.FinishAutofillInteraction
import com.aozijx.passly.feature.autofill.core.ResolveAutofillSelection
import com.aozijx.passly.feature.autofill.core.SaveAutofillCredential
import com.aozijx.passly.feature.autofill.core.StartAutofillInteraction
import com.aozijx.passly.feature.autofill.runtime.AutofillInteractionStore
import com.aozijx.passly.feature.autofill.runtime.AutofillSelectionReceipt
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

sealed interface CredentialManagerUnlockResult {
    data class Rediscovered(
        val discovery: AutofillDiscoveryOutcome,
    ) : CredentialManagerUnlockResult

    data class NotAuthorized(
        val authorization: AutofillAuthorizationOutcome,
    ) : CredentialManagerUnlockResult
}

sealed interface CredentialManagerPasswordResult {
    data class Success(
        val username: String,
        val password: String,
        val receipt: AutofillSelectionReceipt,
    ) : CredentialManagerPasswordResult

    data object NotAvailable : CredentialManagerPasswordResult
}

sealed interface CredentialManagerCreateResult {
    data object Saved : CredentialManagerCreateResult
    data object NotSaved : CredentialManagerCreateResult
    data class NotAuthorized(
        val authorization: AutofillAuthorizationOutcome,
    ) : CredentialManagerCreateResult
}

/**
 * Credential Manager orchestration over the shared Autofill interaction lifecycle.
 *
 * Android request parsing and response construction stay outside this class. The
 * interaction store remains the sole authority for scope, authorization and the
 * candidate IDs that were actually presented to the system picker.
 */
@Singleton
class CredentialManagerFlow @Inject constructor(
    private val discoverCandidates: DiscoverAutofillCandidates,
    private val startInteraction: StartAutofillInteraction,
    private val authorizeInteraction: AuthorizeAutofillInteraction,
    private val resolveSelection: ResolveAutofillSelection,
    private val saveCredential: SaveAutofillCredential,
    private val finishInteraction: FinishAutofillInteraction,
    private val interactionStore: AutofillInteractionStore,
    private val settingsSource: InteractionSettingsSource,
) {
    suspend fun beginGet(request: AutofillRequest): AutofillDiscoveryOutcome =
        discoverCandidates(request)

    suspend fun unlockAndRediscover(
        interactionId: AutofillInteractionId,
        request: AutofillRequest,
    ): CredentialManagerUnlockResult = when (
        val authorization = authorizeInteraction(interactionId)
    ) {
        is AutofillAuthorizationOutcome.Authorized -> CredentialManagerUnlockResult.Rediscovered(
            discoverCandidates(request, interactionId),
        )

        is AutofillAuthorizationOutcome.Cancelled,
        is AutofillAuthorizationOutcome.Failed,
        is AutofillAuthorizationOutcome.Rejected,
        -> CredentialManagerUnlockResult.NotAuthorized(authorization)
    }

    suspend fun selectPassword(
        interactionId: AutofillInteractionId,
        scope: AutofillGrantContext,
        entryId: EntryId,
        allowedUserIds: Set<String> = emptySet(),
    ): CredentialManagerPasswordResult {
        var completed = false
        return try {
            when (
                val selection = resolveSelection(
                    interactionId = interactionId,
                    scope = scope,
                    entryId = entryId,
                    includeOtp = false,
                )
            ) {
                is AutofillSelectionOutcome.Filled -> {
                    val payload = selection.payload
                    if (
                        payload.username.isBlank() ||
                        payload.password.isBlank() ||
                        (allowedUserIds.isNotEmpty() && payload.username !in allowedUserIds)
                    ) {
                        CredentialManagerPasswordResult.NotAvailable
                    } else {
                        CredentialManagerPasswordResult.Success(
                            username = payload.username,
                            password = payload.password,
                            receipt = AutofillSelectionReceipt(interactionId, payload.entryId),
                        )
                            .also { completed = true }
                    }
                }

                AutofillSelectionOutcome.Cancelled,
                AutofillSelectionOutcome.Unauthorized,
                AutofillSelectionOutcome.CandidateUnavailable,
                AutofillSelectionOutcome.ScopeMismatch,
                is AutofillSelectionOutcome.Rejected,
                is AutofillSelectionOutcome.Failed,
                -> CredentialManagerPasswordResult.NotAvailable
            }
        } finally {
            withContext(NonCancellable) {
                if (!completed) {
                    finishInteraction.cancel(interactionId)
                }
            }
        }
    }

    suspend fun beginCreate(scope: AutofillGrantContext): AutofillInteractionSnapshot =
        startInteraction(
            source = AutofillInteractionSource.CREDENTIAL_MANAGER,
            scope = scope,
            // Credential creation is already an explicit system-confirmed action.
            // It only authenticates when the Vault itself is locked.
            requireAuthentication = false,
        )

    suspend fun createPassword(
        interactionId: AutofillInteractionId,
        scope: AutofillGrantContext,
        username: String,
        password: String,
    ): CredentialManagerCreateResult {
        val policy = settingsSource.interaction.first().autofill
        if (!policy.enabled || !policy.credentialManagerEnabled) {
            finishInteraction.cancel(interactionId)
            return CredentialManagerCreateResult.NotSaved
        }
        val snapshot = interactionStore.snapshot(interactionId)
        if (
            snapshot == null ||
            snapshot.source != AutofillInteractionSource.CREDENTIAL_MANAGER ||
            snapshot.scope != scope.normalized()
        ) {
            finishInteraction.cancel(interactionId)
            return CredentialManagerCreateResult.NotSaved
        }

        val authorization = authorizeInteraction(interactionId)
        if (authorization !is AutofillAuthorizationOutcome.Authorized) {
            return CredentialManagerCreateResult.NotAuthorized(authorization)
        }

        var completed = false
        return try {
            when (
                saveCredential(
                    AutofillSaveDraft(
                        packageName = scope.packageName,
                        webDomain = scope.webDomain,
                        pageTitle = null,
                        username = username,
                        password = password,
                    ),
                )
            ) {
                is AutofillSaveOutcome.Saved -> CredentialManagerCreateResult.Saved
                    .also { completed = true }
                AutofillSaveOutcome.InvalidDraft,
                is AutofillSaveOutcome.Failed,
                -> CredentialManagerCreateResult.NotSaved
            }
        } finally {
            withContext(NonCancellable) {
                if (completed) {
                    finishInteraction.complete(interactionId)
                } else {
                    finishInteraction.cancel(interactionId)
                }
            }
        }
    }

    suspend fun cancel(interactionId: AutofillInteractionId) {
        finishInteraction.cancel(interactionId)
    }
}
