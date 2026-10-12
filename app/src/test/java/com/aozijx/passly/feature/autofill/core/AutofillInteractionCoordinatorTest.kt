package com.aozijx.passly.feature.autofill.core

import com.aozijx.passly.domain.access.model.AuthInput
import com.aozijx.passly.domain.access.model.AuthenticationMethod
import com.aozijx.passly.domain.access.model.AuthenticationRequest
import com.aozijx.passly.domain.access.model.AuthenticationResult
import com.aozijx.passly.domain.access.model.AuthenticationState
import com.aozijx.passly.domain.access.model.CancellationReason
import com.aozijx.passly.domain.access.model.LockReason
import com.aozijx.passly.domain.access.port.AuthenticationRequester
import com.aozijx.passly.domain.access.port.SecureSessionAccessState
import com.aozijx.passly.domain.access.port.SessionLockController
import com.aozijx.passly.domain.autofill.model.AutofillAuthorizationOutcome
import com.aozijx.passly.domain.autofill.model.AutofillGrantContext
import com.aozijx.passly.domain.autofill.model.AutofillInteractionRejection
import com.aozijx.passly.domain.autofill.model.AutofillInteractionSource
import com.aozijx.passly.domain.autofill.model.AutofillInteractionState
import com.aozijx.passly.domain.autofill.model.AutofillInteractionUpdate
import com.aozijx.passly.domain.entry.model.EntryId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AutofillInteractionCoordinatorTest {
    @Test
    fun lockedInteractionAuthenticatesOnceAndReusesAuthorizationForSelection() = runBlocking {
        val vault = FakeVault(AuthenticationState.Locked)
        val authentication = FakeAuthenticationController(vault)
        val coordinator = coordinator(authentication, vault)
        val interaction = coordinator.create(
            source = AutofillInteractionSource.LEGACY,
            scope = scope(),
            requireAuthentication = false,
        )

        assertEquals(AutofillInteractionState.RequiresUnlock, interaction.state)
        assertTrue(coordinator.authorize(interaction.id) is AutofillAuthorizationOutcome.Authorized)
        assertTrue(coordinator.authorize(interaction.id) is AutofillAuthorizationOutcome.Authorized)
        assertEquals(1, authentication.authenticationCount)

        val candidateId = EntryId("entry-1")
        coordinator.recordCandidates(interaction.id, setOf(candidateId))
        val selection = coordinator.beginSelection(interaction.id, candidateId)

        assertTrue(selection is AutofillInteractionUpdate.Updated)
        assertEquals(
            AutofillInteractionState.ResolvingSelection(candidateId),
            (selection as AutofillInteractionUpdate.Updated).snapshot.state,
        )
        assertEquals(1, authentication.authenticationCount)
    }

    @Test
    fun cancelledAuthenticationTerminatesGrantAndNextInteractionStartsCleanly() = runBlocking {
        val vault = FakeVault(AuthenticationState.Locked)
        val authentication = FakeAuthenticationController(
            vault = vault,
            results = ArrayDeque(
                listOf(
                    AuthenticationResult.Cancelled(CancellationReason.USER),
                    AuthenticationResult.Success(AuthenticationMethod.APP_PASSWORD),
                ),
            ),
        )
        val coordinator = coordinator(authentication, vault)
        val first = coordinator.create(AutofillInteractionSource.LEGACY, scope(), false)

        assertTrue(coordinator.authorize(first.id) is AutofillAuthorizationOutcome.Cancelled)
        assertEquals(AutofillInteractionState.Cancelled, coordinator.snapshot(first.id)?.state)

        val second = coordinator.create(AutofillInteractionSource.LEGACY, scope(), false)
        assertTrue(coordinator.authorize(second.id) is AutofillAuthorizationOutcome.Authorized)
        assertEquals(2, authentication.authenticationCount)
    }

    @Test
    fun selectionRejectsCandidateThatWasNotPresented() = runBlocking {
        val vault = FakeVault(AuthenticationState.Authenticated(1L))
        val authentication = FakeAuthenticationController(vault)
        val coordinator = coordinator(authentication, vault)
        val interaction = coordinator.create(AutofillInteractionSource.LEGACY, scope(), false)
        coordinator.recordCandidates(interaction.id, setOf(EntryId("entry-1")))

        val result = coordinator.beginSelection(interaction.id, EntryId("entry-2"))

        assertEquals(
            AutofillInteractionUpdate.Rejected(AutofillInteractionRejection.CANDIDATE_NOT_PRESENTED),
            result,
        )
        assertEquals(0, authentication.authenticationCount)
    }

    @Test
    fun cleanupNeverLocksVaultThatWasAlreadyUnlocked() = runBlocking {
        val vault = FakeVault(AuthenticationState.Authenticated(1L))
        val authentication = FakeAuthenticationController(vault)
        val coordinator = coordinator(authentication, vault)
        val interaction = coordinator.create(AutofillInteractionSource.CREDENTIAL_MANAGER, scope(), false)

        coordinator.complete(interaction.id)
        coordinator.complete(interaction.id)

        assertNull(authentication.lastLockReason)
    }

    @Test
    fun sharedOwnedUnlockClosesOnlyAfterLastAuthorizedBorrowerTerminates() = runBlocking {
        val vault = FakeVault(AuthenticationState.Locked)
        val authentication = FakeAuthenticationController(vault)
        val coordinator = coordinator(authentication, vault)
        val owner = coordinator.create(AutofillInteractionSource.LEGACY, scope(), false)
        coordinator.authorize(owner.id)
        val borrower = coordinator.create(
            AutofillInteractionSource.CREDENTIAL_MANAGER,
            scope("com.example.second"),
            false,
        )

        coordinator.complete(owner.id)
        assertNull(authentication.lastLockReason)

        coordinator.complete(borrower.id)
        assertEquals(LockReason.AUTOFILL_REQUEST_FINISHED, authentication.lastLockReason)
    }

    private fun coordinator(
        authentication: FakeAuthenticationController,
        vault: FakeVault,
    ): AutofillInteractionCoordinator = AutofillInteractionCoordinator(
        authenticationRequester = authentication,
        sessionLockController = authentication,
        vaultAccessState = vault,
    )

    private fun scope(packageName: String = "com.example.target") =
        AutofillGrantContext(packageName, "https://example.com/login")

    private class FakeVault(initialState: AuthenticationState) : SecureSessionAccessState {
        private val mutableState = MutableStateFlow(initialState)
        override val authenticationState: StateFlow<AuthenticationState> = mutableState

        override fun isUnlocked(): Boolean = mutableState.value is AuthenticationState.Authenticated

        fun authenticate() {
            mutableState.value = AuthenticationState.Authenticated(1L)
        }

        fun lock() {
            mutableState.value = AuthenticationState.Locked
        }
    }

    private class FakeAuthenticationController(
        private val vault: FakeVault,
        private val results: ArrayDeque<AuthenticationResult> = ArrayDeque(
            listOf(AuthenticationResult.Success(AuthenticationMethod.APP_PASSWORD)),
        ),
    ) : AuthenticationRequester, SessionLockController {
        var authenticationCount: Int = 0
        var lastLockReason: LockReason? = null

        override suspend fun authenticate(
            request: AuthenticationRequest,
            input: AuthInput,
        ): AuthenticationResult {
            authenticationCount++
            val result = results.removeFirstOrNull()
                ?: AuthenticationResult.Success(AuthenticationMethod.APP_PASSWORD)
            if (result is AuthenticationResult.Success) vault.authenticate()
            return result
        }

        override suspend fun lock(reason: LockReason) {
            lastLockReason = reason
            vault.lock()
        }
    }
}
