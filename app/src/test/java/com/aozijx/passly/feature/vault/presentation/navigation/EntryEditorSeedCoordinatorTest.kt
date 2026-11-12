package com.aozijx.passly.feature.vault.presentation.navigation

import androidx.lifecycle.ViewModelStore
import com.aozijx.passly.core.observability.ErrorCodes
import com.aozijx.passly.core.observability.Events
import com.aozijx.passly.core.observability.Reasons
import com.aozijx.passly.domain.access.model.AuthenticationState
import com.aozijx.passly.domain.access.port.SecureSessionAccessState
import com.aozijx.passly.domain.entry.model.credential.WifiSecurityType
import com.aozijx.passly.domain.entry.policy.WifiQrPayload
import com.aozijx.passly.feature.vault.entry.EditorSeedToken
import com.aozijx.passly.feature.vault.entry.EntryEditorSeed
import com.aozijx.passly.feature.vault.entry.EntryEditorSeedConsumeResult
import com.aozijx.passly.feature.vault.entry.EntryEditorSeedStore
import com.aozijx.passly.testing.MainDispatcherRule
import com.aozijx.passly.testing.RecordingTestLogger
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class EntryEditorSeedCoordinatorTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(StandardTestDispatcher())

    private val seed =
        EntryEditorSeed.Wifi(
            WifiQrPayload("Office", "secret-password", WifiSecurityType.WpaPersonal, false),
        )

    private fun coordinator(
        session: MutableSession,
        logger: RecordingTestLogger = RecordingTestLogger(),
    ) = EntryEditorSeedCoordinator(session, EntryEditorSeedStore(), logger)

    @Test
    fun `stage and consume require a full secure session`() {
        val session = MutableSession(AuthenticationState.Locked)
        val coordinator = coordinator(session)

        assertEquals(EntryEditorSeedNavigationResult.AccessDenied, stageSeedResult(coordinator))
        assertEquals(
            EntryEditorSeedConsumeResult.AccessDenied,
            coordinator.consume(EditorSeedToken("missing")),
        )
    }

    @Test
    fun `reauthentication invalidates seed even when collector misses intermediate lock`() =
        runTest {
            val session = MutableSession(AuthenticationState.Authenticated(1L))
            val coordinator = coordinator(session)
            runCurrent()
            val staged = stageSeed(coordinator)
            session.set(AuthenticationState.Locked)
            session.set(AuthenticationState.Authenticated(2L))

            assertEquals(EntryEditorSeedConsumeResult.Missing, coordinator.consume(staged))
        }

    @Test
    fun `new session can stage its own seed without retaining previous token`() =
        runTest {
            val session = MutableSession(AuthenticationState.Authenticated(1L))
            val coordinator = coordinator(session)
            val old = stageSeed(coordinator)
            session.set(AuthenticationState.Authenticated(2L))
            val current = stageSeed(coordinator)

            assertEquals(EntryEditorSeedConsumeResult.Missing, coordinator.consume(old))
            assertEquals(EntryEditorSeedConsumeResult.Consumed(seed), coordinator.consume(current))
        }

    @Test
    fun `clearing view model store rejects late stage and consume calls`() {
        val session = MutableSession(AuthenticationState.Authenticated(1L))
        val coordinator = coordinator(session)
        val store = ViewModelStore()
        store.put("seed-coordinator", coordinator)
        val staged = stageSeed(coordinator)

        store.clear()

        assertEquals(EntryEditorSeedNavigationResult.AccessDenied, stageSeedResult(coordinator))
        assertEquals(EntryEditorSeedConsumeResult.AccessDenied, coordinator.consume(staged))
    }

    @Test
    fun `locking clears an unconsumed seed`() =
        runTest {
            val session = MutableSession(AuthenticationState.Authenticated(1L))
            val coordinator = coordinator(session)
            val staged = stageSeed(coordinator)

            session.set(AuthenticationState.Locked)
            advanceUntilIdle()
            session.set(AuthenticationState.Authenticated(2L))
            advanceUntilIdle()

            assertEquals(EntryEditorSeedConsumeResult.Missing, coordinator.consume(staged))
        }

    @Test
    fun `ordinary navigation exception discards staged token logs and returns failure`() {
        val logger = RecordingTestLogger()
        val coordinator = coordinator(MutableSession(AuthenticationState.Authenticated(1L)), logger)
        val failure = IOException("navigation failed")
        var token: EditorSeedToken? = null

        val result =
            coordinator.navigateWithSeed(
                seed = seed,
                routeForToken = { stagedToken ->
                    token = stagedToken
                    "vault/add/wifi/from-seed/${stagedToken.value}"
                },
                navigate = { throw failure },
            )

        assertEquals(EntryEditorSeedNavigationResult.NavigationFailed(failure), result)
        assertEquals(EntryEditorSeedConsumeResult.Missing, coordinator.consume(requireNotNull(token)))
        assertEquals(1, logger.events.size)
        assertEquals(Events.Vault.Error, logger.events.single().event)
        assertEquals(ErrorCodes.Security.Vault.VaultOperationFailed, logger.events.single().errorCode)
        assertEquals(Reasons.Application.Fallback.UnknownFailure, logger.events.single().reason)
        assertSame(failure, logger.events.single().throwable)
    }

    @Test
    fun `failed old navigation does not discard a newer staged token`() {
        val coordinator = coordinator(MutableSession(AuthenticationState.Authenticated(1L)))
        var replacement: EditorSeedToken? = null

        val result =
            coordinator.navigateWithSeed(
                seed = seed,
                routeForToken = { "old/${it.value}" },
                navigate = {
                    coordinator.navigateWithSeed(
                        seed = seed,
                        routeForToken = { token ->
                            replacement = token
                            "new/${token.value}"
                        },
                        navigate = {},
                    )
                    throw IOException("old navigation failed")
                },
            )

        assertTrue(result is EntryEditorSeedNavigationResult.NavigationFailed)
        assertEquals(EntryEditorSeedConsumeResult.Consumed(seed), coordinator.consume(requireNotNull(replacement)))
    }

    @Test
    fun `cancellation and fatal navigation failures discard seed and propagate`() {
        val coordinator = coordinator(MutableSession(AuthenticationState.Authenticated(1L)))
        val cancellation = CancellationException("navigation cancelled")
        var cancelledToken: EditorSeedToken? = null
        val observedCancellation =
            try {
                coordinator.navigateWithSeed(
                    seed,
                    routeForToken = {
                        cancelledToken = it
                        "cancel/${it.value}"
                    },
                    navigate = { throw cancellation },
                )
                null
            } catch (failure: CancellationException) {
                failure
            }

        val fatal = LinkageError("navigation fatal")
        var fatalToken: EditorSeedToken? = null
        val observedFatal =
            try {
                coordinator.navigateWithSeed(
                    seed,
                    routeForToken = {
                        fatalToken = it
                        "fatal/${it.value}"
                    },
                    navigate = { throw fatal },
                )
                null
            } catch (failure: LinkageError) {
                failure
            }

        assertSame(cancellation, observedCancellation)
        assertSame(fatal, observedFatal)
        assertEquals(EntryEditorSeedConsumeResult.Missing, coordinator.consume(requireNotNull(cancelledToken)))
        assertEquals(EntryEditorSeedConsumeResult.Missing, coordinator.consume(requireNotNull(fatalToken)))
    }

    private fun stageSeedResult(coordinator: EntryEditorSeedCoordinator): EntryEditorSeedNavigationResult =
        coordinator.navigateWithSeed(
            seed = seed,
            routeForToken = { "test/${it.value}" },
            navigate = {},
        )

    private fun stageSeed(coordinator: EntryEditorSeedCoordinator): EditorSeedToken {
        var token: EditorSeedToken? = null
        val result =
            coordinator.navigateWithSeed(
                seed = seed,
                routeForToken = { stagedToken ->
                    token = stagedToken
                    "test/${stagedToken.value}"
                },
                navigate = {},
            )
        assertEquals(EntryEditorSeedNavigationResult.Navigated, result)
        return requireNotNull(token)
    }

    private class MutableSession(
        initial: AuthenticationState,
    ) : SecureSessionAccessState {
        private val state = MutableStateFlow(initial)
        override val authenticationState: StateFlow<AuthenticationState> = state

        override fun isUnlocked(): Boolean = state.value is AuthenticationState.Authenticated

        fun set(value: AuthenticationState) {
            state.value = value
        }
    }
}
