package com.aozijx.passly.feature.autofill.legacy

import com.aozijx.passly.domain.access.model.AuthInput
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
import com.aozijx.passly.domain.autofill.model.AutofillGrantContext
import com.aozijx.passly.domain.autofill.model.AutofillInteractionSource
import com.aozijx.passly.domain.autofill.model.AutofillRequest
import com.aozijx.passly.domain.autofill.model.AutofillSaveDraft
import com.aozijx.passly.domain.autofill.model.AutofillSaveOutcome
import com.aozijx.passly.domain.autofill.model.AutofillSource
import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.settings.model.AutofillSettings
import com.aozijx.passly.domain.settings.model.InteractionSettings
import com.aozijx.passly.domain.settings.port.InteractionSettingsSource
import com.aozijx.passly.feature.autofill.core.AuthorizeAutofillInteraction
import com.aozijx.passly.feature.autofill.core.AutofillSaveDraftExtraction
import com.aozijx.passly.feature.autofill.core.FinishAutofillInteraction
import com.aozijx.passly.feature.autofill.core.StartAutofillInteraction
import com.aozijx.passly.feature.autofill.legacy.runtime.LegacySaveDraftStore
import com.aozijx.passly.feature.autofill.legacy.runtime.LegacySaveToken
import com.aozijx.passly.feature.autofill.runtime.AutofillInteractionStore
import com.aozijx.passly.feature.autofill.runtime.AutofillUnlockLease
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class LegacySaveFlowTest {
    @Test
    fun unlockedFullSessionPersistsImmediately() = runBlocking {
        val fixture = fixture(AuthenticationState.Authenticated(1L))

        assertEquals(LegacySavePreparation.Saved, fixture.flow.prepare(request()))
        assertEquals(1, fixture.saveCalls)
        assertEquals(0, fixture.authentication.authenticationCount)
    }

    @Test
    fun `immediate persistence exception reaches the service boundary unchanged`() = runBlocking {
        val failure = IllegalStateException("password=must-not-be-logged")
        val fixture = fixture(
            initialState = AuthenticationState.Authenticated(1L),
            saveFailure = failure,
        )

        val thrown = try {
            fixture.flow.prepare(request())
            null
        } catch (caught: IllegalStateException) {
            caught
        }

        assertSame(failure, thrown)
    }

    @Test
    fun lockedSessionStagesWithoutWriting() = runBlocking {
        val fixture = fixture(AuthenticationState.Locked)

        val result = fixture.flow.prepare(request())

        assertTrue(result is LegacySavePreparation.Staged)
        assertEquals(0, fixture.saveCalls)
    }

    @Test
    fun disabledPolicyRejectsPrepareAndConsumedContinuation() = runBlocking {
        val fixture = fixture(AuthenticationState.Locked)
        fixture.settings.value = settings(saveEnabled = false)
        assertEquals(LegacySavePreparation.Disabled, fixture.flow.prepare(request()))

        fixture.settings.value = settings(saveEnabled = true)
        val staged = fixture.flow.prepare(request()) as LegacySavePreparation.Staged
        fixture.settings.value = settings(saveEnabled = false)

        assertEquals(LegacySaveBeginResult.Disabled, fixture.flow.beginContinuation(staged.token))
        fixture.settings.value = settings(saveEnabled = true)
        assertEquals(LegacySaveBeginResult.Unavailable, fixture.flow.beginContinuation(staged.token))
    }

    @Test
    fun missingAndExpiredTokensAreUnavailable() = runBlocking {
        var now = 0L
        val fixture = fixture(AuthenticationState.Locked, nowMillis = { now })

        assertEquals(
            LegacySaveBeginResult.Unavailable,
            fixture.flow.beginContinuation(LegacySaveToken("missing")),
        )
        val staged = fixture.flow.prepare(request()) as LegacySavePreparation.Staged
        now = LegacySaveDraftStore.TTL_MILLIS
        assertEquals(LegacySaveBeginResult.Unavailable, fixture.flow.beginContinuation(staged.token))
    }

    @Test
    fun discardedContinuationCannotBeConsumed() = runBlocking {
        val fixture = fixture(AuthenticationState.Locked)
        val staged = fixture.flow.prepare(request()) as LegacySavePreparation.Staged

        fixture.flow.discard(staged.token)

        assertEquals(LegacySaveBeginResult.Unavailable, fixture.flow.beginContinuation(staged.token))
    }

    @Test
    fun lockedContinuationAuthenticatesOnceAndPersistsOnce() = runBlocking {
        val fixture = fixture(AuthenticationState.Locked)
        val continuation = fixture.stageAndBegin()

        assertEquals(LegacySaveCompletion.Saved, fixture.flow.completeContinuation(continuation))
        assertEquals(1, fixture.authentication.authenticationCount)
        assertEquals(1, fixture.saveCalls)
        assertEquals(LockReason.AUTOFILL_REQUEST_FINISHED, fixture.authentication.lastLockReason)
    }

    @Test
    fun duplicateContinuationCanWriteOnlyOnce() = runBlocking {
        val fixture = fixture(AuthenticationState.Locked)
        val continuation = fixture.stageAndBegin()

        val completions = listOf(
            async { fixture.flow.completeContinuation(continuation) },
            async { fixture.flow.completeContinuation(continuation) },
        ).awaitAll()

        assertEquals(1, completions.count { it == LegacySaveCompletion.Saved })
        assertEquals(1, fixture.saveCalls)
    }

    @Test
    fun authenticationCancellationAndPersistenceFailureTerminateOnlySaveInteraction() = runBlocking {
        val cancelled = fixture(
            AuthenticationState.Locked,
            authenticationResults = ArrayDeque(
                listOf(AuthenticationResult.Cancelled(CancellationReason.USER)),
            ),
        )
        val cancelledContinuation = cancelled.stageAndBegin()
        assertEquals(
            LegacySaveCompletion.NotAuthorized,
            cancelled.flow.completeContinuation(cancelledContinuation),
        )

        val failed = fixture(
            AuthenticationState.Authenticated(1L),
            saveOutcome = AutofillSaveOutcome.Failed(
                com.aozijx.passly.domain.autofill.model.AutofillOperationFailure.PERSISTENCE_FAILED,
            ),
        )
        failed.vault.lock()
        val failedContinuation = failed.stageAndBegin()
        assertEquals(LegacySaveCompletion.NotSaved, failed.flow.completeContinuation(failedContinuation))
        assertEquals(1, failed.saveCalls)
    }

    @Test
    fun preExistingFullSessionIsNeverClosed() = runBlocking {
        val fixture = fixture(AuthenticationState.Authenticated(1L))
        fixture.vault.lock()
        val staged = fixture.flow.prepare(request()) as LegacySavePreparation.Staged
        fixture.vault.fullAuthenticate()
        val continuation = (fixture.flow.beginContinuation(staged.token) as LegacySaveBeginResult.Ready)
            .continuation

        assertEquals(LegacySaveCompletion.Saved, fixture.flow.completeContinuation(continuation))
        assertNull(fixture.authentication.lastLockReason)
        assertTrue(fixture.vault.hasFullSecureSessionAccess())
    }

    private fun fixture(
        initialState: AuthenticationState,
        nowMillis: () -> Long = { 0L },
        authenticationResults: ArrayDeque<AuthenticationResult> = ArrayDeque(
            listOf(AuthenticationResult.Success(AuthenticationMethod.APP_PASSWORD)),
        ),
        saveOutcome: AutofillSaveOutcome = AutofillSaveOutcome.Saved(EntryId("saved")),
        saveFailure: RuntimeException? = null,
    ): Fixture {
        val settings = MutableStateFlow(settings())
        val vault = FakeVault(initialState)
        val authentication = FakeAuthenticationController(vault, authenticationResults)
        val store = AutofillInteractionStore()
        val lease = AutofillUnlockLease(authentication)
        val draftStore = LegacySaveDraftStore(nowMillis) { LegacySaveToken("token") }
        var saveCalls = 0
        val flow = LegacySaveFlow(
            settingsSource = object : InteractionSettingsSource {
                override val interaction = settings
            },
            canPersistImmediately = vault::hasFullSecureSessionAccess,
            draftStore = draftStore,
            startInteraction = StartAutofillInteraction(store, lease, vault),
            authorizeInteraction = AuthorizeAutofillInteraction(authentication, vault, store, lease),
            finishInteraction = FinishAutofillInteraction(store, lease),
            interactionStore = store,
            extractDraft = { AutofillSaveDraftExtraction.Ready(draft()) },
            saveCredential = {
                saveCalls++
                saveFailure?.let { throw it }
                saveOutcome
            },
        )
        return Fixture(flow, settings, vault, authentication) { saveCalls }
    }

    private fun settings(saveEnabled: Boolean = true) = InteractionSettings(
        autofill = AutofillSettings(enabled = true, savePromptsEnabled = saveEnabled),
    )

    private fun request() = AutofillRequest(
        packageName = "com.example",
        domain = "example.com",
        fields = emptyList(),
        source = AutofillSource.AUTOFILL_SERVICE,
    )

    private fun draft() = AutofillSaveDraft(
        packageName = "com.example",
        webDomain = "example.com",
        pageTitle = null,
        username = "user",
        password = "password",
    )

    private data class Fixture(
        val flow: LegacySaveFlow,
        val settings: MutableStateFlow<InteractionSettings>,
        val vault: FakeVault,
        val authentication: FakeAuthenticationController,
        val saveCallsProvider: () -> Int,
    ) {
        val saveCalls: Int get() = saveCallsProvider()

        suspend fun stageAndBegin(): LegacySaveContinuation {
            val staged = flow.prepare(
                AutofillRequest(
                    packageName = "com.example",
                    domain = "example.com",
                    fields = emptyList(),
                    source = AutofillSource.AUTOFILL_SERVICE,
                ),
            ) as LegacySavePreparation.Staged
            return (flow.beginContinuation(staged.token) as LegacySaveBeginResult.Ready).continuation
        }
    }

    private class FakeVault(initialState: AuthenticationState) : SecureSessionAccessState {
        private val state = MutableStateFlow(initialState)
        override val authenticationState: StateFlow<AuthenticationState> = state
        override fun isUnlocked() = state.value is AuthenticationState.Authenticated ||
            state.value is AuthenticationState.AutofillAuthenticated

        fun autofillAuthenticate() {
            state.value = AuthenticationState.AutofillAuthenticated(1L)
        }

        fun fullAuthenticate() {
            state.value = AuthenticationState.Authenticated(1L)
        }

        fun lock() {
            state.value = AuthenticationState.Locked
        }
    }

    private class FakeAuthenticationController(
        private val vault: FakeVault,
        private val results: ArrayDeque<AuthenticationResult>,
    ) : AuthenticationRequester, SessionLockController {
        var authenticationCount = 0
        var lastLockReason: LockReason? = null

        override suspend fun authenticate(
            request: AuthenticationRequest,
            input: AuthInput,
        ): AuthenticationResult {
            authenticationCount++
            return (results.removeFirstOrNull()
                ?: AuthenticationResult.Success(AuthenticationMethod.APP_PASSWORD)).also {
                if (it is AuthenticationResult.Success) vault.autofillAuthenticate()
            }
        }

        override suspend fun lock(request: SessionLockRequest) {
            lastLockReason = request.reason
            vault.lock()
        }
    }
}
