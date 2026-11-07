package com.aozijx.passly.feature.autofill.core

import com.aozijx.passly.domain.access.model.AuthInput
import com.aozijx.passly.domain.access.model.AuthenticationMethod
import com.aozijx.passly.domain.access.model.AuthenticationPurpose
import com.aozijx.passly.domain.access.model.AuthenticationRequest
import com.aozijx.passly.domain.access.model.AuthenticationResult
import com.aozijx.passly.domain.access.model.AuthenticationState
import com.aozijx.passly.domain.access.model.FreshAuthenticationRequiredException
import com.aozijx.passly.domain.access.model.SessionLockRequest
import com.aozijx.passly.domain.access.port.AuthenticationRequester
import com.aozijx.passly.domain.access.port.SecureSessionAccessState
import com.aozijx.passly.domain.access.port.SessionLockController
import com.aozijx.passly.domain.autofill.model.AutofillCredentialPayload
import com.aozijx.passly.domain.autofill.model.AutofillCredentialResolution
import com.aozijx.passly.domain.autofill.model.AutofillCredentialSelection
import com.aozijx.passly.domain.autofill.model.AutofillGrantContext
import com.aozijx.passly.domain.autofill.model.AutofillInteractionRejection
import com.aozijx.passly.domain.autofill.model.AutofillInteractionSource
import com.aozijx.passly.domain.autofill.model.AutofillInteractionState
import com.aozijx.passly.domain.autofill.model.AutofillInteractionUpdate
import com.aozijx.passly.domain.autofill.model.AutofillSelectionOutcome
import com.aozijx.passly.domain.autofill.port.AutofillCredentialResolver
import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.feature.autofill.runtime.AutofillInteractionStore
import com.aozijx.passly.feature.autofill.runtime.AutofillUnlockLease
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ResolveAutofillSelectionTest {
    @Test
    fun equivalentUrlAndPackageFormattingKeepsTheSameInteractionScope() =
        runBlocking {
            val fixture = fixture()
            val interaction =
                fixture.start(
                    AutofillInteractionSource.LEGACY,
                    webScope(),
                    requireAuthentication = false,
                )
            assertTrue(
                fixture.store.confirmWebRecipient(interaction.id, webScope(), 1L) is AutofillInteractionUpdate.Updated,
            )
            fixture.store.recordCandidates(interaction.id, setOf(EntryId("entry-1")), sessionAtMs = 1L)
            fixture.store.confirmWebEntry(interaction.id, webScope(), EntryId("entry-1"), 1L)

            val outcome =
                fixture.operation(
                    interaction.id,
                    AutofillGrantContext(" COM.EXAMPLE.APP ", "HTTPS://EXAMPLE.COM:443/login"),
                    EntryId("entry-1"),
                    includeOtp = false,
                )

            assertEquals(AutofillSelectionOutcome.CandidateUnavailable, outcome)
            assertEquals(1, fixture.resolver.resolveCalls)
        }

    @Test
    fun scopeMismatchIsRejectedBeforeCredentialResolution() =
        runBlocking {
            val fixture = fixture()
            val interaction =
                fixture.start(
                    AutofillInteractionSource.LEGACY,
                    scope(),
                    requireAuthentication = false,
                )
            fixture.store.recordCandidates(interaction.id, setOf(EntryId("entry-1")), sessionAtMs = 1L)

            val outcome =
                fixture.operation(
                    interaction.id,
                    scope("com.example.other"),
                    EntryId("entry-1"),
                    includeOtp = false,
                )

            assertEquals(AutofillSelectionOutcome.ScopeMismatch, outcome)
            assertEquals(0, fixture.resolver.resolveCalls)
        }

    @Test
    fun unpresentedCandidateIsRejectedBeforeCredentialResolution() =
        runBlocking {
            val fixture = fixture()
            val interaction =
                fixture.start(
                    AutofillInteractionSource.LEGACY,
                    scope(),
                    requireAuthentication = false,
                )
            fixture.store.recordCandidates(interaction.id, setOf(EntryId("entry-1")))

            val outcome =
                fixture.operation(
                    interaction.id,
                    scope(),
                    EntryId("entry-2"),
                    includeOtp = false,
                )

            assertEquals(
                AutofillSelectionOutcome.Rejected(AutofillInteractionRejection.CANDIDATE_NOT_PRESENTED),
                outcome,
            )
            assertEquals(0, fixture.resolver.resolveCalls)
        }

    @Test
    fun freshAuthenticationRequiredCannotResolveSecretBeforeAuthorization() =
        runBlocking {
            val fixture = fixture(authenticationState = AuthenticationState.Authenticated(1L))
            val interaction =
                fixture.start(
                    AutofillInteractionSource.LEGACY,
                    scope(),
                    requireAuthentication = true,
                )

            val beforeAuthentication =
                fixture.operation(
                    interaction.id,
                    scope(),
                    EntryId("entry-1"),
                    includeOtp = false,
                )

            assertEquals(AutofillSelectionOutcome.Unauthorized, beforeAuthentication)
            assertEquals(0, fixture.resolver.resolveCalls)

            fixture.store.beginAuthorization(interaction.id)
            fixture.store.markAuthorized(interaction.id)
            fixture.store.recordCandidates(interaction.id, setOf(EntryId("entry-1")))

            val afterAuthentication =
                fixture.operation(
                    interaction.id,
                    scope(),
                    EntryId("entry-1"),
                    includeOtp = false,
                )

            assertEquals(AutofillSelectionOutcome.CandidateUnavailable, afterAuthentication)
            assertEquals(1, fixture.resolver.resolveCalls)
        }

    @Test
    fun presentedCandidateResolvesTypedPayload() =
        runBlocking {
            val payload =
                AutofillCredentialPayload(
                    entryId = EntryId("entry-1"),
                    username = "alice",
                    password = "secret",
                )
            val fixture = fixture(AutofillCredentialResolution.Resolved(payload))
            val interaction =
                fixture.start(
                    AutofillInteractionSource.CREDENTIAL_MANAGER,
                    scope(),
                    requireAuthentication = false,
                )
            fixture.store.recordCandidates(interaction.id, setOf(payload.entryId))

            val outcome =
                fixture.operation(
                    interaction.id,
                    scope(),
                    payload.entryId,
                    includeOtp = false,
                )

            assertEquals(AutofillSelectionOutcome.Filled(payload), outcome)
            assertEquals(1, fixture.resolver.resolveCalls)
            assertTrue(fixture.resolver.lastSelection?.includeOtp == false)
        }

    @Test
    fun unverifiedLegacyWebSelectionCannotReadCredentialBeforeExplicitEntryConfirmation() =
        runBlocking {
            val payload =
                AutofillCredentialPayload(
                    entryId = EntryId("entry-1"),
                    username = "alice",
                    password = "secret",
                )
            val fixture = fixture(AutofillCredentialResolution.Resolved(payload))
            val interaction =
                fixture.start(
                    AutofillInteractionSource.LEGACY,
                    webScope(),
                    requireAuthentication = false,
                )
            assertEquals(
                AutofillInteractionUpdate.Rejected(AutofillInteractionRejection.WEB_ORIGIN_NOT_CONFIRMED),
                fixture.store.recordCandidates(interaction.id, setOf(payload.entryId)),
            )
            fixture.store.confirmWebRecipient(interaction.id, webScope(), 1L)
            fixture.store.recordCandidates(interaction.id, setOf(payload.entryId), sessionAtMs = 1L)
            val outcome =
                fixture.operation(
                    interaction.id,
                    webScope(),
                    payload.entryId,
                    includeOtp = false,
                )

            assertTrue(outcome !is AutofillSelectionOutcome.Filled)
            assertEquals(
                AutofillSelectionOutcome.Rejected(AutofillInteractionRejection.WEB_ENTRY_NOT_CONFIRMED),
                outcome,
            )
            assertEquals(0, fixture.resolver.resolveCalls)
        }

    @Test
    fun webConsentFromAnEarlierVaultSessionCannotReadCredential() =
        runBlocking {
            val fixture =
                fixture(
                    AutofillCredentialResolution.Resolved(
                        AutofillCredentialPayload(EntryId("entry-1"), "alice", "secret"),
                    ),
                )
            val interaction = fixture.start(AutofillInteractionSource.LEGACY, webScope(), requireAuthentication = false)
            fixture.store.confirmWebRecipient(interaction.id, webScope(), sessionAtMs = 2L)
            fixture.store.recordCandidates(interaction.id, setOf(EntryId("entry-1")), sessionAtMs = 2L)
            fixture.store.confirmWebEntry(interaction.id, webScope(), EntryId("entry-1"), sessionAtMs = 2L)

            val outcome = fixture.operation(interaction.id, webScope(), EntryId("entry-1"), includeOtp = false)

            assertEquals(
                AutofillSelectionOutcome.Rejected(AutofillInteractionRejection.WEB_CONFIRMATION_EXPIRED),
                outcome,
            )
            assertEquals(0, fixture.resolver.resolveCalls)
        }

    @Test
    fun expiredSensitiveKeyRequestsAuthenticationAndRetriesSelection() =
        runBlocking {
            val payload =
                AutofillCredentialPayload(
                    entryId = EntryId("entry-1"),
                    username = "alice",
                    password = "secret",
                )
            val fixture =
                fixture(
                    result = AutofillCredentialResolution.Resolved(payload),
                    authenticationResult = AuthenticationResult.Success(AuthenticationMethod.APP_PASSWORD),
                )
            fixture.resolver.freshAuthenticationRequiredOnFirstCall = true
            val selectionScope = AutofillGrantContext("com.example.app")
            val interaction =
                fixture.start(
                    AutofillInteractionSource.LEGACY,
                    selectionScope,
                    requireAuthentication = false,
                )
            fixture.store.recordCandidates(interaction.id, setOf(payload.entryId))

            val outcome =
                fixture.operation(
                    interaction.id,
                    selectionScope,
                    payload.entryId,
                    includeOtp = false,
                )

            assertEquals(AutofillSelectionOutcome.Filled(payload), outcome)
            assertEquals(2, fixture.resolver.resolveCalls)
            assertEquals(
                AuthenticationPurpose.AUTOFILL,
                fixture.authentication
                    .requests
                    .single()
                    .purpose,
            )
        }

    @Test
    fun interactionCancelledDuringFreshAuthenticationDoesNotResolveSecretAgain() =
        runBlocking {
            val fixture =
                fixture(
                    authenticationResult = AuthenticationResult.Success(AuthenticationMethod.APP_PASSWORD),
                )
            fixture.resolver.freshAuthenticationRequiredOnFirstCall = true
            val selectionScope = AutofillGrantContext("com.example.app")
            val interaction =
                fixture.start(
                    AutofillInteractionSource.LEGACY,
                    selectionScope,
                    requireAuthentication = false,
                )
            fixture.store.recordCandidates(interaction.id, setOf(EntryId("entry-1")))
            fixture.authentication.beforeResult = {
                val before = requireNotNull(fixture.store.snapshot(interaction.id))
                assertEquals(selectionScope, before.scope)
                assertEquals(
                    AutofillInteractionState.ResolvingSelection(EntryId("entry-1")),
                    before.state,
                )
                fixture.store.finish(
                    interaction.id,
                    AutofillInteractionState.Cancelled,
                )
            }

            val outcome =
                fixture.operation(
                    interaction.id,
                    selectionScope,
                    EntryId("entry-1"),
                    includeOtp = false,
                )

            assertEquals(AutofillSelectionOutcome.Cancelled, outcome)
            assertEquals(1, fixture.resolver.resolveCalls)
        }

    private fun fixture(
        result: AutofillCredentialResolution = AutofillCredentialResolution.NotFound,
        authenticationState: AuthenticationState = AuthenticationState.Authenticated(1L),
        authenticationResult: AuthenticationResult? = null,
    ): Fixture {
        val vault = UnlockedVault(authenticationState)
        val authentication = RecordingAuthentication(authenticationResult)
        val store = AutofillInteractionStore()
        val lease = AutofillUnlockLease(authentication)
        val resolver = RecordingCredentialResolver(result)
        return Fixture(
            store = store,
            start = StartAutofillInteraction(store, lease, vault),
            resolver = resolver,
            authentication = authentication,
            operation = ResolveAutofillSelection(store, resolver, authentication, vault),
        )
    }

    private fun scope(packageName: String = "com.example.app") = AutofillGrantContext(packageName)

    private fun webScope(packageName: String = "com.example.app") = AutofillGrantContext(packageName, "example.com")

    private data class Fixture(
        val store: AutofillInteractionStore,
        val start: StartAutofillInteraction,
        val resolver: RecordingCredentialResolver,
        val authentication: RecordingAuthentication,
        val operation: ResolveAutofillSelection,
    )

    private class RecordingCredentialResolver(
        private val result: AutofillCredentialResolution,
    ) : AutofillCredentialResolver {
        var resolveCalls = 0
        var lastSelection: AutofillCredentialSelection? = null
        var freshAuthenticationRequiredOnFirstCall = false

        override suspend fun resolve(selection: AutofillCredentialSelection): AutofillCredentialResolution {
            resolveCalls++
            lastSelection = selection
            if (freshAuthenticationRequiredOnFirstCall && resolveCalls == 1) {
                throw FreshAuthenticationRequiredException()
            }
            return result
        }
    }

    private class UnlockedVault(
        private val state: AuthenticationState,
    ) : SecureSessionAccessState {
        override val authenticationState: StateFlow<AuthenticationState> = MutableStateFlow(state)

        override fun isUnlocked(): Boolean = state is AuthenticationState.Authenticated
    }

    private class RecordingAuthentication(
        private val result: AuthenticationResult?,
    ) : AuthenticationRequester,
        SessionLockController {
        val requests = mutableListOf<AuthenticationRequest>()
        var beforeResult: suspend () -> Unit = {}

        override suspend fun authenticate(
            request: AuthenticationRequest,
            input: AuthInput,
        ): AuthenticationResult {
            requests += request
            beforeResult()
            return result ?: error("Authentication is not expected")
        }

        override suspend fun lock(request: SessionLockRequest) = Unit
    }
}
