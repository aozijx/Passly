package com.aozijx.passly.feature.vault.presentation.navigation

import com.aozijx.passly.domain.access.model.AuthenticationState
import com.aozijx.passly.domain.access.port.SecureSessionAccessState
import com.aozijx.passly.domain.entry.model.credential.WifiSecurityType
import com.aozijx.passly.feature.vault.entry.EditorSeedToken
import com.aozijx.passly.feature.vault.entry.EntryEditorSeed
import com.aozijx.passly.feature.vault.entry.EntryEditorSeedConsumeResult
import com.aozijx.passly.feature.vault.entry.EntryEditorSeedStore
import com.aozijx.passly.domain.entry.policy.WifiQrPayload
import com.aozijx.passly.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class EntryEditorSeedCoordinatorTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(StandardTestDispatcher())

    private val seed = EntryEditorSeed.Wifi(
        WifiQrPayload("Office", "secret-password", WifiSecurityType.WpaPersonal, false),
    )

    @Test
    fun `stage and consume require a full secure session`() {
        val session = MutableSession(AuthenticationState.Locked)
        val coordinator = EntryEditorSeedCoordinator(session, EntryEditorSeedStore())

        assertEquals(EntryEditorSeedStageResult.AccessDenied, coordinator.stage(seed))
        assertEquals(
            EntryEditorSeedConsumeResult.AccessDenied,
            coordinator.consume(EditorSeedToken("missing")),
        )
    }

    @Test
    fun `reauthentication invalidates seed even when collector misses intermediate lock`() = runTest {
        val session = MutableSession(AuthenticationState.Authenticated(1L))
        val coordinator = EntryEditorSeedCoordinator(session, EntryEditorSeedStore())
        runCurrent()
        val staged = coordinator.stage(seed) as EntryEditorSeedStageResult.Staged
        session.set(AuthenticationState.Locked)
        session.set(AuthenticationState.Authenticated(2L))

        assertEquals(EntryEditorSeedConsumeResult.Missing, coordinator.consume(staged.token))
    }

    @Test
    fun `new session can stage its own seed without retaining previous token`() = runTest {
        val session = MutableSession(AuthenticationState.Authenticated(1L))
        val coordinator = EntryEditorSeedCoordinator(session, EntryEditorSeedStore())
        val old = coordinator.stage(seed) as EntryEditorSeedStageResult.Staged
        session.set(AuthenticationState.Authenticated(2L))
        val current = coordinator.stage(seed) as EntryEditorSeedStageResult.Staged

        assertEquals(EntryEditorSeedConsumeResult.Missing, coordinator.consume(old.token))
        assertEquals(EntryEditorSeedConsumeResult.Consumed(seed), coordinator.consume(current.token))
    }

    @Test
    fun `destroyed coordinator rejects late stage and consume calls`() {
        val session = MutableSession(AuthenticationState.Authenticated(1L))
        val coordinator = EntryEditorSeedCoordinator(session, EntryEditorSeedStore())
        val staged = coordinator.stage(seed) as EntryEditorSeedStageResult.Staged
        coordinator.javaClass.getDeclaredMethod("onCleared").apply { isAccessible = true }.invoke(coordinator)

        assertEquals(EntryEditorSeedStageResult.AccessDenied, coordinator.stage(seed))
        assertEquals(EntryEditorSeedConsumeResult.AccessDenied, coordinator.consume(staged.token))
    }

    @Test
    fun `locking clears an unconsumed seed`() = runTest {
        val session = MutableSession(AuthenticationState.Authenticated(1L))
        val coordinator = EntryEditorSeedCoordinator(session, EntryEditorSeedStore())
        val staged = coordinator.stage(seed) as EntryEditorSeedStageResult.Staged

        session.set(AuthenticationState.Locked)
        advanceUntilIdle()
        session.set(AuthenticationState.Authenticated(2L))
        advanceUntilIdle()

        assertEquals(EntryEditorSeedConsumeResult.Missing, coordinator.consume(staged.token))
    }

    private class MutableSession(initial: AuthenticationState) : SecureSessionAccessState {
        private val state = MutableStateFlow(initial)
        override val authenticationState: StateFlow<AuthenticationState> = state
        override fun isUnlocked(): Boolean = state.value is AuthenticationState.Authenticated
        fun set(value: AuthenticationState) {
            state.value = value
        }
    }
}
