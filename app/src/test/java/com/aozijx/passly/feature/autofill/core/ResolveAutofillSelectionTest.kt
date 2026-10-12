package com.aozijx.passly.feature.autofill.core

import com.aozijx.passly.domain.access.model.AuthInput
import com.aozijx.passly.domain.access.model.AuthenticationRequest
import com.aozijx.passly.domain.access.model.AuthenticationResult
import com.aozijx.passly.domain.access.model.AuthenticationState
import com.aozijx.passly.domain.access.model.LockReason
import com.aozijx.passly.domain.access.port.AuthenticationRequester
import com.aozijx.passly.domain.access.port.SecureSessionAccessState
import com.aozijx.passly.domain.access.port.SessionLockController
import com.aozijx.passly.domain.autofill.model.AutofillCredentialPayload
import com.aozijx.passly.domain.autofill.model.AutofillCredentialResolution
import com.aozijx.passly.domain.autofill.model.AutofillCredentialSelection
import com.aozijx.passly.domain.autofill.model.AutofillGrantContext
import com.aozijx.passly.domain.autofill.model.AutofillInteractionRejection
import com.aozijx.passly.domain.autofill.model.AutofillInteractionSource
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
    fun scopeMismatchIsRejectedBeforeCredentialResolution() = runBlocking {
        val fixture = fixture()
        val interaction = fixture.start(
            AutofillInteractionSource.LEGACY,
            scope(),
            requireAuthentication = false,
        )
        fixture.store.recordCandidates(interaction.id, setOf(EntryId("entry-1")))

        val outcome = fixture.operation(
            interaction.id,
            scope("com.example.other"),
            EntryId("entry-1"),
            includeOtp = false,
        )

        assertEquals(AutofillSelectionOutcome.ScopeMismatch, outcome)
        assertEquals(0, fixture.resolver.resolveCalls)
    }

    @Test
    fun unpresentedCandidateIsRejectedBeforeCredentialResolution() = runBlocking {
        val fixture = fixture()
        val interaction = fixture.start(
            AutofillInteractionSource.LEGACY,
            scope(),
            requireAuthentication = false,
        )
        fixture.store.recordCandidates(interaction.id, setOf(EntryId("entry-1")))

        val outcome = fixture.operation(
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
    fun presentedCandidateResolvesTypedPayload() = runBlocking {
        val payload = AutofillCredentialPayload(
            entryId = EntryId("entry-1"),
            username = "alice",
            password = "secret",
        )
        val fixture = fixture(AutofillCredentialResolution.Resolved(payload))
        val interaction = fixture.start(
            AutofillInteractionSource.CREDENTIAL_MANAGER,
            scope(),
            requireAuthentication = false,
        )
        fixture.store.recordCandidates(interaction.id, setOf(payload.entryId))

        val outcome = fixture.operation(
            interaction.id,
            scope(),
            payload.entryId,
            includeOtp = false,
        )

        assertEquals(AutofillSelectionOutcome.Filled(payload), outcome)
        assertEquals(1, fixture.resolver.resolveCalls)
        assertTrue(fixture.resolver.lastSelection?.includeOtp == false)
    }

    private fun fixture(
        result: AutofillCredentialResolution = AutofillCredentialResolution.NotFound,
    ): Fixture {
        val vault = UnlockedVault()
        val authentication = NoOpAuthentication()
        val store = AutofillInteractionStore()
        val lease = AutofillUnlockLease(authentication)
        val resolver = RecordingCredentialResolver(result)
        return Fixture(
            store = store,
            start = StartAutofillInteraction(store, lease, vault),
            resolver = resolver,
            operation = ResolveAutofillSelection(store, resolver),
        )
    }

    private fun scope(packageName: String = "com.example.app") =
        AutofillGrantContext(packageName, "example.com")

    private data class Fixture(
        val store: AutofillInteractionStore,
        val start: StartAutofillInteraction,
        val resolver: RecordingCredentialResolver,
        val operation: ResolveAutofillSelection,
    )

    private class RecordingCredentialResolver(
        private val result: AutofillCredentialResolution,
    ) : AutofillCredentialResolver {
        var resolveCalls = 0
        var lastSelection: AutofillCredentialSelection? = null

        override suspend fun resolve(selection: AutofillCredentialSelection): AutofillCredentialResolution {
            resolveCalls++
            lastSelection = selection
            return result
        }
    }

    private class UnlockedVault : SecureSessionAccessState {
        override val authenticationState: StateFlow<AuthenticationState> =
            MutableStateFlow(AuthenticationState.Authenticated(1L))

        override fun isUnlocked(): Boolean = true
    }

    private class NoOpAuthentication : AuthenticationRequester, SessionLockController {
        override suspend fun authenticate(request: AuthenticationRequest, input: AuthInput): AuthenticationResult =
            error("Authentication is not expected")

        override suspend fun lock(reason: LockReason) = Unit
    }
}
