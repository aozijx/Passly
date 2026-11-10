package com.aozijx.passly.feature.autofill.core

import com.aozijx.passly.core.observability.AutofillLogSnapshot
import com.aozijx.passly.core.observability.LogStatements
import com.aozijx.passly.core.observability.Logger
import com.aozijx.passly.core.observability.Reasons
import com.aozijx.passly.domain.access.port.SecureSessionAccessState
import com.aozijx.passly.domain.autofill.model.AUTOFILL_CANDIDATE_LIMIT
import com.aozijx.passly.domain.autofill.model.AutofillCandidateQuery
import com.aozijx.passly.domain.autofill.model.AutofillDiscoveryContext
import com.aozijx.passly.domain.autofill.model.AutofillDiscoveryOutcome
import com.aozijx.passly.domain.autofill.model.AutofillGrantContext
import com.aozijx.passly.domain.autofill.model.AutofillInteractionId
import com.aozijx.passly.domain.autofill.model.AutofillInteractionSnapshot
import com.aozijx.passly.domain.autofill.model.AutofillInteractionSource
import com.aozijx.passly.domain.autofill.model.AutofillInteractionState
import com.aozijx.passly.domain.autofill.model.AutofillWebConsent
import com.aozijx.passly.domain.autofill.model.AutofillInteractionUpdate
import com.aozijx.passly.domain.autofill.model.AutofillOperationFailure
import com.aozijx.passly.domain.autofill.model.AutofillRequest
import com.aozijx.passly.domain.autofill.model.AutofillSource
import com.aozijx.passly.domain.autofill.model.FieldRole
import com.aozijx.passly.domain.autofill.port.AutofillCandidateRepository
import com.aozijx.passly.domain.autofill.port.FieldMatchStrategy
import com.aozijx.passly.domain.autofill.port.MatchResult
import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.settings.model.AutofillSettings
import com.aozijx.passly.domain.settings.port.AutofillSettingsSource
import com.aozijx.passly.feature.autofill.internal.di.Heuristic
import com.aozijx.passly.feature.autofill.internal.di.Strict
import com.aozijx.passly.feature.autofill.runtime.AutofillInteractionStore
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

@Singleton
internal class DiscoverAutofillCandidates
    @Inject
    constructor(
        private val settingsSource: AutofillSettingsSource,
        private val candidateRepository: AutofillCandidateRepository,
        @param:Heuristic private val legacyFieldMatchStrategy: FieldMatchStrategy,
        @param:Strict private val credentialFieldMatchStrategy: FieldMatchStrategy,
        private val startInteraction: StartAutofillInteraction,
        private val interactionStore: AutofillInteractionStore,
        private val secureSessionAccessState: SecureSessionAccessState,
        private val finishInteraction: FinishAutofillInteraction,
        private val logger: Logger,
    ) {
        suspend operator fun invoke(
            request: AutofillRequest,
            interactionId: AutofillInteractionId? = null,
            classifiedRoles: Map<String, FieldRole>? = null,
            directEntryId: EntryId? = null,
        ): AutofillDiscoveryOutcome {
            val policy = settingsSource.autofillSettings.first()
            if (!policy.enabled || !policy.supports(request.source)) {
                return AutofillDiscoveryOutcome.Disabled
            }
            if (request.source == AutofillSource.AUTOFILL_SERVICE && request.ambiguousWebOrigin) {
                return AutofillDiscoveryOutcome.Failed(AutofillOperationFailure.AMBIGUOUS_WEB_ORIGIN)
            }

            val match =
                classifiedRoles?.let { roles ->
                    MatchResult(
                        roleMap = roles,
                        hasCredentials =
                            roles.values.any {
                                it == FieldRole.USERNAME || it == FieldRole.PASSWORD || it == FieldRole.OTP
                            },
                    )
                } ?: request.source.fieldMatchStrategy().match(request)
            logger.log(
                LogStatements.Autofill.fieldsClassified(
                    source = request.source,
                    targetPackage = request.packageName,
                    snapshot =
                        AutofillLogSnapshot.Classification(
                            fieldCount = request.fields.size,
                            roles =
                                AutofillLogSnapshot.Classification.Roles(
                                    username = match.roleMap.values.count { it == FieldRole.USERNAME },
                                    password = match.roleMap.values.count { it == FieldRole.PASSWORD },
                                    otp = match.roleMap.values.count { it == FieldRole.OTP },
                                ),
                        ),
                ),
            )
            if (!match.hasCredentials) return AutofillDiscoveryOutcome.UnsupportedFields

            val scope =
                AutofillGrantContext(request.packageName, request.domain).normalized()
                    ?: return AutofillDiscoveryOutcome.Failed(AutofillOperationFailure.INVALID_SCOPE)
            val snapshot =
                resolveInteraction(request, scope, policy, interactionId)
                    ?: return AutofillDiscoveryOutcome.Failed(AutofillOperationFailure.INTERACTION_NOT_FOUND)
            try {
                currentCoroutineContext().ensureActive()
                if (snapshot.scope != scope) {
                    return AutofillDiscoveryOutcome.Failed(AutofillOperationFailure.SCOPE_MISMATCH)
                }
                val context =
                    AutofillDiscoveryContext(
                        roleMap = match.roleMap,
                        presentation = policy.presentation,
                        requireAuthentication = policy.requireAuthentication,
                    )
                if (snapshot.state == AutofillInteractionState.RequiresUnlock) {
                    return AutofillDiscoveryOutcome.RequiresUnlock(snapshot, context)
                }
                if (snapshot.state != AutofillInteractionState.Authorized &&
                    snapshot.state != AutofillInteractionState.CandidatesReady
                ) {
                    return AutofillDiscoveryOutcome.Failed(AutofillOperationFailure.INVALID_INTERACTION_STATE)
                }
                if (request.source == AutofillSource.AUTOFILL_SERVICE &&
                    scope.webDomain != null &&
                    snapshot.webConsent is AutofillWebConsent.Unconfirmed
                ) {
                    return AutofillDiscoveryOutcome.RequiresWebRecipientConfirmation(snapshot, context)
                }
                val sessionGeneration = secureSessionAccessState.autofillSessionGeneration()
                if (request.source == AutofillSource.AUTOFILL_SERVICE &&
                    scope.webDomain != null &&
                    (snapshot.webConsent as? AutofillWebConsent.Confirmed)?.sessionAtMs != sessionGeneration
                ) {
                    finishInteraction.cancel(snapshot.id)
                    return AutofillDiscoveryOutcome.Failed(AutofillOperationFailure.INVALID_INTERACTION_STATE)
                }
                if (directEntryId != null &&
                    snapshot.state == AutofillInteractionState.CandidatesReady &&
                    scope.webDomain == null
                ) {
                    return AutofillDiscoveryOutcome.ResumeSelection(snapshot, directEntryId)
                }

                val candidates =
                    try {
                        candidateRepository.search(
                            AutofillCandidateQuery(
                                packageName = scope.packageName.takeIf { scope.webDomain == null },
                                webDomain = scope.webDomain,
                                limit = AUTOFILL_CANDIDATE_LIMIT,
                                strictDomainMatching = policy.strictDomainMatching,
                            ),
                        )
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (error: Exception) {
                        finishInteraction.cancel(snapshot.id)
                        logger.log(
                            LogStatements.Autofill.discoveryFailed(
                                error,
                                Reasons.DatabaseUnavailable,
                                LogStatements.Autofill.context(request.source, request.packageName),
                            ),
                        )
                        return AutofillDiscoveryOutcome.Failed(AutofillOperationFailure.CANDIDATE_QUERY_FAILED)
                    }

                currentCoroutineContext().ensureActive()
                val activeSessionGeneration = secureSessionAccessState.autofillSessionGeneration()
                if (scope.webDomain != null && activeSessionGeneration != sessionGeneration) {
                    finishInteraction.cancel(snapshot.id)
                    return AutofillDiscoveryOutcome.Failed(AutofillOperationFailure.INVALID_INTERACTION_STATE)
                }

                val recorded =
                    interactionStore.recordCandidates(
                        snapshot.id,
                        candidates.mapTo(linkedSetOf()) { it.entry.id },
                        sessionAtMs = activeSessionGeneration,
                    )
                val recordedSnapshot =
                    when (recorded) {
                        is AutofillInteractionUpdate.Updated -> {
                            recorded.snapshot
                        }

                        is AutofillInteractionUpdate.Rejected -> {
                            return AutofillDiscoveryOutcome.Failed(
                                AutofillOperationFailure.INVALID_INTERACTION_STATE,
                            )
                        }
                    }
                currentCoroutineContext().ensureActive()
                return if (candidates.isEmpty()) {
                    AutofillDiscoveryOutcome.NoCandidates(recordedSnapshot, context)
                } else {
                    AutofillDiscoveryOutcome.Candidates(recordedSnapshot, candidates, context)
                }
            } catch (cancelled: CancellationException) {
                try {
                    withContext(NonCancellable) { finishInteraction.cancel(snapshot.id) }
                } catch (cleanupFailure: Exception) {
                    cancelled.addSuppressed(cleanupFailure)
                }
                throw cancelled
            }
        }

        private suspend fun resolveInteraction(
            request: AutofillRequest,
            scope: AutofillGrantContext,
            policy: AutofillSettings,
            interactionId: AutofillInteractionId?,
        ): AutofillInteractionSnapshot? =
            if (interactionId == null) {
                startInteraction(
                    source = request.source.toInteractionSource(),
                    scope = scope,
                    requireAuthentication = policy.requireAuthentication,
                )
            } else {
                interactionStore.snapshot(interactionId)
            }

        private fun AutofillSettings.supports(source: AutofillSource): Boolean =
            when (source) {
                AutofillSource.AUTOFILL_SERVICE -> true
                AutofillSource.CREDENTIAL_MANAGER -> credentialManagerEnabled
                AutofillSource.ACCESSIBILITY -> true
            }

        private fun AutofillSource.toInteractionSource(): AutofillInteractionSource =
            when (this) {
                AutofillSource.AUTOFILL_SERVICE -> AutofillInteractionSource.LEGACY
                AutofillSource.CREDENTIAL_MANAGER -> AutofillInteractionSource.CREDENTIAL_MANAGER
                AutofillSource.ACCESSIBILITY -> AutofillInteractionSource.ACCESSIBILITY
            }

        private fun AutofillSource.fieldMatchStrategy(): FieldMatchStrategy =
            when (this) {
                AutofillSource.AUTOFILL_SERVICE -> legacyFieldMatchStrategy
                AutofillSource.CREDENTIAL_MANAGER -> credentialFieldMatchStrategy
                AutofillSource.ACCESSIBILITY -> legacyFieldMatchStrategy
            }
    }
