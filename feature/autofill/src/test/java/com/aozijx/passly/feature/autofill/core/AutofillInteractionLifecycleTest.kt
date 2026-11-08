package com.aozijx.passly.feature.autofill.core

import com.aozijx.passly.domain.access.model.AuthInput
import com.aozijx.passly.domain.access.model.AuthenticationFailure
import com.aozijx.passly.domain.access.model.AuthenticationFailureCode
import com.aozijx.passly.domain.access.model.AuthenticationMethod
import com.aozijx.passly.domain.access.model.AuthenticationRequest
import com.aozijx.passly.domain.access.model.AuthenticationResult
import com.aozijx.passly.domain.access.model.AuthenticationState
import com.aozijx.passly.domain.access.model.CancellationReason
import com.aozijx.passly.domain.access.model.LockReason
import com.aozijx.passly.domain.access.model.SessionLockRequest
import com.aozijx.passly.domain.access.port.AuthenticationRequester
import com.aozijx.passly.domain.access.port.SecureSessionAccessState
import com.aozijx.passly.domain.access.port.SessionLockController
import com.aozijx.passly.domain.autofill.model.AutofillAuthorizationOutcome
import com.aozijx.passly.domain.autofill.model.AutofillGrantContext
import com.aozijx.passly.domain.autofill.model.AutofillInteractionId
import com.aozijx.passly.domain.autofill.model.AutofillInteractionRejection
import com.aozijx.passly.domain.autofill.model.AutofillInteractionSource
import com.aozijx.passly.domain.autofill.model.AutofillInteractionState
import com.aozijx.passly.domain.autofill.model.AutofillInteractionUpdate
import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.feature.autofill.runtime.AutofillInteractionStore
import com.aozijx.passly.feature.autofill.runtime.AutofillInteractionTimeout
import com.aozijx.passly.feature.autofill.runtime.AutofillUnlockLease
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class AutofillInteractionLifecycleTest {
    @Test
    fun interactionAuthorizationRequiresFullSessionOrOwnedAutofillLease() =
        runBlocking {
            val recovery = fixture(AuthenticationState.RecoveryMode(authenticatedAtMs = 0L))
            val recoveryInteraction =
                recovery.start(
                    AutofillInteractionSource.LEGACY,
                    scope(),
                    requireAuthentication = false,
                )
            assertEquals(AutofillInteractionState.RequiresUnlock, recoveryInteraction.state)
            assertEquals(0, recovery.authentication.authenticationCount)

            val activeSession = fixture(AuthenticationState.AutofillAuthenticated(authenticatedAtMs = 0L))
            activeSession.lease.acquireOrBorrow(AutofillInteractionId("existing-owner"), acquiredUnlock = true)
            val borrowedInteraction =
                activeSession.start(
                    AutofillInteractionSource.LEGACY,
                    scope(),
                    requireAuthentication = false,
                )
            assertEquals(AutofillInteractionState.Authorized, borrowedInteraction.state)
            assertEquals(0, activeSession.authentication.authenticationCount)

            val orphanedSession = fixture(AuthenticationState.AutofillAuthenticated(authenticatedAtMs = 0L))
            val orphanedInteraction =
                orphanedSession.start(
                    AutofillInteractionSource.LEGACY,
                    scope(),
                    requireAuthentication = false,
                )
            assertEquals(AutofillInteractionState.RequiresUnlock, orphanedInteraction.state)
            assertEquals(0, orphanedSession.authentication.authenticationCount)
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun interactionTimeoutExpiresStateAndReleasesOwnedUnlock() =
        runTest {
            val vault = FakeVault(AuthenticationState.Locked)
            val authentication =
                FakeAuthenticationController(
                    vault,
                    ArrayDeque(listOf(AuthenticationResult.Success(AuthenticationMethod.APP_PASSWORD))),
                )
            val store = AutofillInteractionStore()
            val lease = AutofillUnlockLease(authentication)
            val timeout =
                AutofillInteractionTimeout(
                    store = store,
                    unlockLease = lease,
                    scope = this,
                    timeoutMillis = 60_000L,
                )
            val start = StartAutofillInteraction(store, lease, vault, timeout)
            val authorize = AuthorizeAutofillInteraction(authentication, vault, store, lease)
            val interaction = start(AutofillInteractionSource.LEGACY, scope(), false)
            authorize(interaction.id)

            advanceTimeBy(60_000L)
            runCurrent()

            assertNull(store.snapshot(interaction.id))
            assertEquals(LockReason.AUTOFILL_REQUEST_FINISHED, authentication.lastLockReason)
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun interactionTimeoutDefersWhileSecretResolutionIsActive() =
        runTest {
            val vault = FakeVault(AuthenticationState.Authenticated(1L))
            val authentication = FakeAuthenticationController(vault, ArrayDeque())
            val store = AutofillInteractionStore()
            val lease = AutofillUnlockLease(authentication)
            val timeout = AutofillInteractionTimeout(store, lease, backgroundScope, 60_000L)
            val start = StartAutofillInteraction(store, lease, vault, timeout)
            val interaction = start(AutofillInteractionSource.LEGACY, scope(), false)
            val entryId = EntryId("entry-1")
            store.recordCandidates(interaction.id, setOf(entryId))
            store.beginSelection(interaction.id, entryId)

            advanceTimeBy(60_000L)
            runCurrent()

            assertEquals(
                AutofillInteractionState.ResolvingSelection(entryId),
                store.snapshot(interaction.id)?.state,
            )
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun interactionTimeoutDoesNotExpireAnActiveAuthenticationPrompt() =
        runTest {
            val vault = FakeVault(AuthenticationState.Locked)
            val authentication = DeferredAuthenticationController(vault)
            val store = AutofillInteractionStore()
            val lease = AutofillUnlockLease(authentication)
            val timeout = AutofillInteractionTimeout(store, lease, backgroundScope, 60_000L)
            val start = StartAutofillInteraction(store, lease, vault, timeout)
            val authorize = AuthorizeAutofillInteraction(authentication, vault, store, lease)
            val interaction = start(AutofillInteractionSource.LEGACY, scope(), false)
            val authorization = async { authorize(interaction.id) }
            authentication.started.await()

            advanceTimeBy(60_000L)
            runCurrent()

            assertEquals(
                AutofillInteractionState.Authenticating,
                store.snapshot(interaction.id)?.state,
            )
            authentication.result.complete(
                AuthenticationResult.Success(AuthenticationMethod.APP_PASSWORD),
            )
            assertTrue(authorization.await() is AutofillAuthorizationOutcome.Authorized)
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun interactionTimeoutRestartsAfterAuthenticationCompletes() =
        runTest {
            val vault = FakeVault(AuthenticationState.Locked)
            val authentication = DeferredAuthenticationController(vault)
            val store = AutofillInteractionStore()
            val lease = AutofillUnlockLease(authentication)
            val timeout = AutofillInteractionTimeout(store, lease, backgroundScope, 60_000L)
            val start = StartAutofillInteraction(store, lease, vault, timeout)
            val authorize = AuthorizeAutofillInteraction(authentication, vault, store, lease, timeout)
            val interaction = start(AutofillInteractionSource.LEGACY, scope(), false)
            val authorization = async { authorize(interaction.id) }
            authentication.started.await()

            advanceTimeBy(50_000L)
            authentication.result.complete(
                AuthenticationResult.Success(AuthenticationMethod.APP_PASSWORD),
            )
            assertTrue(authorization.await() is AutofillAuthorizationOutcome.Authorized)

            advanceTimeBy(10_000L)
            runCurrent()

            assertEquals(AutofillInteractionState.Authorized, store.snapshot(interaction.id)?.state)

            advanceTimeBy(50_000L)
            runCurrent()

            assertNull(store.snapshot(interaction.id))
            assertEquals(LockReason.AUTOFILL_REQUEST_FINISHED, authentication.lastLockReason)
        }

    @Test
    fun lockedInteractionAuthenticatesOnceAndReusesAuthorizationForSelection() =
        runBlocking {
            val fixture = fixture(AuthenticationState.Locked)
            val interaction =
                fixture.start(
                    AutofillInteractionSource.LEGACY,
                    scope(),
                    requireAuthentication = false,
                )

            assertEquals(AutofillInteractionState.RequiresUnlock, interaction.state)
            assertTrue(fixture.authorize(interaction.id) is AutofillAuthorizationOutcome.Authorized)
            assertTrue(fixture.authorize(interaction.id) is AutofillAuthorizationOutcome.Authorized)
            assertEquals(1, fixture.authentication.authenticationCount)

            val candidateId = EntryId("entry-1")
            fixture.store.recordCandidates(interaction.id, setOf(candidateId))
            val selection = fixture.store.beginSelection(interaction.id, candidateId)

            assertTrue(selection is AutofillInteractionUpdate.Updated)
            assertEquals(
                AutofillInteractionState.ResolvingSelection(candidateId),
                (selection as AutofillInteractionUpdate.Updated).snapshot.state,
            )
        }

    @Test
    fun thrownAuthenticationFailureTerminatesSharedAttemptAndAllowsRetry() =
        runTest {
            val vault = FakeVault(AuthenticationState.Locked)
            val authentication = ThrowOnceAuthenticationController(vault)
            val store = AutofillInteractionStore()
            val lease = AutofillUnlockLease(authentication)
            val start = StartAutofillInteraction(store, lease, vault)
            val authorize = AuthorizeAutofillInteraction(authentication, vault, store, lease)
            val interaction = start(AutofillInteractionSource.LEGACY, scope(), false)

            val owner = async { runCatching { authorize(interaction.id) } }
            authentication.firstAttemptStarted.await()
            val waiter = async { runCatching { authorize(interaction.id) } }
            runCurrent()

            val failure = IllegalStateException("authentication host failed")
            authentication.failFirstAttempt.complete(failure)

            val ownerFailure = requireNotNull(owner.await().exceptionOrNull())
            val waiterFailure = requireNotNull(waiter.await().exceptionOrNull())
            assertEquals(failure::class, ownerFailure::class)
            assertEquals(failure.message, ownerFailure.message)
            assertEquals(failure::class, waiterFailure::class)
            assertEquals(failure.message, waiterFailure.message)
            assertEquals(
                AutofillInteractionState.Failed(
                    AuthenticationFailure(AuthenticationFailureCode.SESSION_TRANSITION_FAILED),
                ),
                store.snapshot(interaction.id)?.state,
            )
            assertEquals(AuthenticationState.Locked, vault.authenticationState.value)
            assertEquals(1, authentication.lockCount)

            val retry = start(AutofillInteractionSource.LEGACY, scope(), false)
            assertTrue(authorize(retry.id) is AutofillAuthorizationOutcome.Authorized)
            assertEquals(2, authentication.authenticationCount)
        }

    @Test
    fun fatalAuthenticationFailureIsSharedAndTerminatesAttempt() =
        runTest {
            val vault = FakeVault(AuthenticationState.Locked)
            val authentication = ThrowOnceAuthenticationController(vault)
            val store = AutofillInteractionStore()
            val lease = AutofillUnlockLease(authentication)
            val start = StartAutofillInteraction(store, lease, vault)
            val interaction = start(AutofillInteractionSource.LEGACY, scope(), false)
            val authorize = AuthorizeAutofillInteraction(authentication, vault, store, lease)

            val owner = async { runCatching { authorize(interaction.id) } }
            authentication.firstAttemptStarted.await()
            val waiter = async { runCatching { authorize(interaction.id) } }
            runCurrent()
            val fatalFailure = AssertionError("fatal authentication failure")
            authentication.failFirstAttempt.complete(fatalFailure)

            assertSame(fatalFailure, owner.await().exceptionOrNull())
            assertSame(fatalFailure, waiter.await().exceptionOrNull())
            assertEquals(
                AutofillInteractionState.Failed(
                    AuthenticationFailure(AuthenticationFailureCode.SESSION_TRANSITION_FAILED),
                ),
                store.snapshot(interaction.id)?.state,
            )
            assertEquals(AuthenticationState.Locked, vault.authenticationState.value)
            assertEquals(1, authentication.lockCount)
        }

    @Test
    fun fatalLeaseCleanupFailurePropagatesAndCompletesSharedWaiter() =
        runTest {
            val vault = FakeVault(AuthenticationState.Locked)
            val cleanupFailure = AssertionError("lease cleanup failed")
            val authentication = ThrowOnceAuthenticationController(vault, cleanupFailure)
            val store = AutofillInteractionStore()
            val lease = AutofillUnlockLease(authentication)
            val start = StartAutofillInteraction(store, lease, vault)
            val authorize = AuthorizeAutofillInteraction(authentication, vault, store, lease)
            val interaction = start(AutofillInteractionSource.LEGACY, scope(), false)

            val owner = async { runCatching { authorize(interaction.id) } }
            authentication.firstAttemptStarted.await()
            val waiter = async { runCatching { authorize(interaction.id) } }
            runCurrent()

            val authenticationFailure = IllegalStateException("authentication host failed")
            authentication.failFirstAttempt.complete(authenticationFailure)

            val ownerFailure = owner.await().exceptionOrNull()
            val waiterFailure = waiter.await().exceptionOrNull()
            assertSame(cleanupFailure, ownerFailure)
            assertSame(cleanupFailure, waiterFailure)
            assertEquals(
                AutofillInteractionState.Failed(
                    AuthenticationFailure(AuthenticationFailureCode.SESSION_TRANSITION_FAILED),
                ),
                store.snapshot(interaction.id)?.state,
            )
            assertEquals(AuthenticationState.Locked, vault.authenticationState.value)
            assertEquals(1, authentication.lockCount)
        }

    @Test
    fun cancelledAuthenticationCompletesSharedWaiterAndAllowsRetry() =
        runTest {
            val vault = FakeVault(AuthenticationState.Locked)
            val authentication = ThrowOnceAuthenticationController(vault)
            val store = AutofillInteractionStore()
            val lease = AutofillUnlockLease(authentication)
            val start = StartAutofillInteraction(store, lease, vault)
            val authorize = AuthorizeAutofillInteraction(authentication, vault, store, lease)
            val interaction = start(AutofillInteractionSource.LEGACY, scope(), false)

            val owner = async { runCatching { authorize(interaction.id) } }
            authentication.firstAttemptStarted.await()
            val waiter = async { authorize(interaction.id) }
            runCurrent()
            val cancellation = CancellationException("caller cancelled")
            authentication.failFirstAttempt.complete(cancellation)

            assertSame(cancellation, owner.await().exceptionOrNull())
            assertTrue(waiter.await() is AutofillAuthorizationOutcome.Cancelled)
            assertEquals(AutofillInteractionState.Cancelled, store.snapshot(interaction.id)?.state)
            assertEquals(AuthenticationState.Locked, vault.authenticationState.value)
            val retry = start(AutofillInteractionSource.LEGACY, scope(), false)
            assertTrue(authorize(retry.id) is AutofillAuthorizationOutcome.Authorized)
            assertEquals(2, authentication.authenticationCount)
        }

    @Test
    fun cancelledAuthenticationTerminatesGrantAndNextInteractionStartsCleanly() =
        runBlocking {
            val fixture =
                fixture(
                    initialState = AuthenticationState.Locked,
                    results =
                        ArrayDeque(
                            listOf(
                                AuthenticationResult.Cancelled(CancellationReason.USER),
                                AuthenticationResult.Success(AuthenticationMethod.APP_PASSWORD),
                            ),
                        ),
                )
            val first = fixture.start(AutofillInteractionSource.LEGACY, scope(), false)

            assertTrue(fixture.authorize(first.id) is AutofillAuthorizationOutcome.Cancelled)
            assertEquals(AutofillInteractionState.Cancelled, fixture.store.snapshot(first.id)?.state)

            val second = fixture.start(AutofillInteractionSource.LEGACY, scope(), false)
            assertTrue(fixture.authorize(second.id) is AutofillAuthorizationOutcome.Authorized)
            assertEquals(2, fixture.authentication.authenticationCount)
        }

    @Test
    fun storeRejectsCandidateThatWasNotPresented() =
        runBlocking {
            val fixture = fixture(AuthenticationState.Authenticated(1L))
            val interaction = fixture.start(AutofillInteractionSource.LEGACY, scope(), false)
            fixture.store.recordCandidates(interaction.id, setOf(EntryId("entry-1")))

            val result = fixture.store.beginSelection(interaction.id, EntryId("entry-2"))

            assertEquals(
                AutofillInteractionUpdate.Rejected(AutofillInteractionRejection.CANDIDATE_NOT_PRESENTED),
                result,
            )
            assertEquals(0, fixture.authentication.authenticationCount)
        }

    @Test
    fun finishingNeverLocksVaultThatWasAlreadyUnlocked() =
        runBlocking {
            val fixture = fixture(AuthenticationState.Authenticated(1L))
            val interaction = fixture.start(AutofillInteractionSource.CREDENTIAL_MANAGER, scope(), false)

            fixture.finish.complete(interaction.id)
            fixture.finish.complete(interaction.id)

            assertNull(fixture.authentication.lastLockReason)
        }

    @Test
    fun sharedUnlockLeaseClosesAfterLastBorrowerFinishes() =
        runBlocking {
            val fixture = fixture(AuthenticationState.Locked)
            val owner = fixture.start(AutofillInteractionSource.LEGACY, scope(), false)
            fixture.authorize(owner.id)
            val borrower =
                fixture.start(
                    AutofillInteractionSource.CREDENTIAL_MANAGER,
                    scope("com.example.second"),
                    false,
                )

            fixture.finish.complete(owner.id)
            assertNull(fixture.authentication.lastLockReason)

            fixture.finish.complete(borrower.id)
            assertEquals(LockReason.AUTOFILL_REQUEST_FINISHED, fixture.authentication.lastLockReason)
        }

    @Test
    fun finishingDuringAuthenticationClosesLateSuccessfulUnlock() =
        runBlocking {
            val vault = FakeVault(AuthenticationState.Locked)
            val authentication = DeferredAuthenticationController(vault)
            val store = AutofillInteractionStore()
            val lease = AutofillUnlockLease(authentication)
            val start = StartAutofillInteraction(store, lease, vault)
            val authorize = AuthorizeAutofillInteraction(authentication, vault, store, lease)
            val finish = FinishAutofillInteraction(store, lease)
            val interaction = start(AutofillInteractionSource.LEGACY, scope(), false)

            val authorization = async { authorize(interaction.id) }
            authentication.started.await()
            finish.cancel(interaction.id)
            authentication.result.complete(
                AuthenticationResult.Success(AuthenticationMethod.APP_PASSWORD),
            )
            authorization.await()

            assertEquals(LockReason.AUTOFILL_REQUEST_FINISHED, authentication.lastLockReason)
            assertEquals(AutofillInteractionState.Cancelled, store.snapshot(interaction.id)?.state)
        }

    @Test
    fun freshAuthenticationAfterDeliveredResultOwnsTheReplacementAutofillSession() =
        runBlocking {
            val vault = FakeVault(AuthenticationState.AutofillAuthenticated(1L))
            val authentication = DeferredAuthenticationController(vault)
            val store = AutofillInteractionStore()
            val lease = AutofillUnlockLease(authentication)
            val start = StartAutofillInteraction(store, lease, vault)
            val authorize = AuthorizeAutofillInteraction(authentication, vault, store, lease)
            val finish = FinishAutofillInteraction(store, lease)
            val delivered =
                store.create(
                    AutofillInteractionSource.LEGACY,
                    scope(),
                    AutofillInteractionState.Authorized,
                )
            lease.acquireOrBorrow(delivered.id, acquiredUnlock = true)
            lease.beginResultCompletion(delivered.id)
            val nextRequest =
                async {
                    start(AutofillInteractionSource.LEGACY, scope(), false)
                }
            finish.complete(delivered.id)
            lease.endResultCompletion {}
            val next = nextRequest.await()

            val authorization = async { authorize(next.id) }
            authentication.started.await()
            authentication.result.complete(
                AuthenticationResult.Success(AuthenticationMethod.APP_PASSWORD),
            )
            assertTrue(authorization.await() is AutofillAuthorizationOutcome.Authorized)
            finish.complete(next.id)

            assertEquals(2, authentication.lockCount)
            assertEquals(AuthenticationState.Locked, vault.authenticationState.value)
        }

    @Test
    fun freshAuthenticationAfterDeliveredResultStartsANewBorrowingWindow() =
        runBlocking {
            val fixture = fixture(AuthenticationState.AutofillAuthenticated(1L))
            val delivered =
                fixture.store.create(
                    AutofillInteractionSource.LEGACY,
                    scope(),
                    AutofillInteractionState.Authorized,
                )
            fixture.lease.acquireOrBorrow(delivered.id, acquiredUnlock = true)
            fixture.lease.beginResultCompletion(delivered.id)
            val nextRequest =
                async {
                    fixture.start(AutofillInteractionSource.LEGACY, scope(), false)
                }
            fixture.finish.complete(delivered.id)
            fixture.lease.endResultCompletion {}
            val authenticated = nextRequest.await()
            assertEquals(AutofillInteractionState.RequiresUnlock, authenticated.state)

            assertTrue(fixture.authorize(authenticated.id) is AutofillAuthorizationOutcome.Authorized)
            val sibling =
                fixture.start(
                    AutofillInteractionSource.CREDENTIAL_MANAGER,
                    scope("com.example.second"),
                    false,
                )

            assertEquals(AutofillInteractionState.Authorized, sibling.state)
        }

    private fun fixture(
        initialState: AuthenticationState,
        results: ArrayDeque<AuthenticationResult> =
            ArrayDeque(
                listOf(AuthenticationResult.Success(AuthenticationMethod.APP_PASSWORD)),
            ),
    ): Fixture {
        val vault = FakeVault(initialState)
        val authentication = FakeAuthenticationController(vault, results)
        val store = AutofillInteractionStore()
        val lease = AutofillUnlockLease(authentication)
        return Fixture(
            store = store,
            authentication = authentication,
            lease = lease,
            start = StartAutofillInteraction(store, lease, vault),
            authorize = AuthorizeAutofillInteraction(authentication, vault, store, lease),
            finish = FinishAutofillInteraction(store, lease),
        )
    }

    private fun scope(packageName: String = "com.example.target") = AutofillGrantContext(packageName)

    private data class Fixture(
        val store: AutofillInteractionStore,
        val authentication: FakeAuthenticationController,
        val lease: AutofillUnlockLease,
        val start: StartAutofillInteraction,
        val authorize: AuthorizeAutofillInteraction,
        val finish: FinishAutofillInteraction,
    )

    private class FakeVault(
        initialState: AuthenticationState,
    ) : SecureSessionAccessState {
        private val mutableState = MutableStateFlow(initialState)
        override val authenticationState: StateFlow<AuthenticationState> = mutableState

        override fun isUnlocked(): Boolean =
            when (mutableState.value) {
                is AuthenticationState.Authenticated,
                is AuthenticationState.AutofillAuthenticated,
                -> true

                else -> false
            }

        fun authenticate() {
            mutableState.value = AuthenticationState.AutofillAuthenticated(1L)
        }

        fun lock() {
            mutableState.value = AuthenticationState.Locked
        }
    }

    private class FakeAuthenticationController(
        private val vault: FakeVault,
        private val results: ArrayDeque<AuthenticationResult>,
    ) : AuthenticationRequester,
        SessionLockController {
        var authenticationCount: Int = 0
        var lastLockReason: LockReason? = null

        override suspend fun authenticate(
            request: AuthenticationRequest,
            input: AuthInput,
        ): AuthenticationResult {
            authenticationCount++
            val result =
                results.removeFirstOrNull()
                    ?: AuthenticationResult.Success(AuthenticationMethod.APP_PASSWORD)
            if (result is AuthenticationResult.Success) vault.authenticate()
            return result
        }

        override suspend fun lock(request: SessionLockRequest) {
            lastLockReason = request.reason
            vault.lock()
        }
    }

    private class DeferredAuthenticationController(
        private val vault: FakeVault,
    ) : AuthenticationRequester,
        SessionLockController {
        val started = CompletableDeferred<Unit>()
        val result = CompletableDeferred<AuthenticationResult>()
        var lastLockReason: LockReason? = null
        var lockCount: Int = 0

        override suspend fun authenticate(
            request: AuthenticationRequest,
            input: AuthInput,
        ): AuthenticationResult {
            started.complete(Unit)
            return result.await().also {
                if (it is AuthenticationResult.Success) vault.authenticate()
            }
        }

        override suspend fun lock(request: SessionLockRequest) {
            lastLockReason = request.reason
            lockCount++
            vault.lock()
        }
    }

    private class ThrowOnceAuthenticationController(
        private val vault: FakeVault,
        private val lockFailure: Throwable? = null,
    ) : AuthenticationRequester,
        SessionLockController {
        val firstAttemptStarted = CompletableDeferred<Unit>()
        val failFirstAttempt = CompletableDeferred<Throwable>()
        var authenticationCount = 0
        var lockCount = 0

        override suspend fun authenticate(
            request: AuthenticationRequest,
            input: AuthInput,
        ): AuthenticationResult {
            authenticationCount++
            if (authenticationCount == 1) {
                vault.authenticate()
                firstAttemptStarted.complete(Unit)
                throw failFirstAttempt.await()
            }
            vault.authenticate()
            return AuthenticationResult.Success(AuthenticationMethod.APP_PASSWORD)
        }

        override suspend fun lock(request: SessionLockRequest) {
            lockCount++
            vault.lock()
            lockFailure?.let { throw it }
        }
    }
}
