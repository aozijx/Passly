package com.aozijx.passly.feature.shell.presentation

import androidx.lifecycle.ViewModelStore
import com.aozijx.passly.domain.access.model.AuthenticationState
import com.aozijx.passly.domain.access.model.SessionLockRequest
import com.aozijx.passly.domain.access.port.DatabaseSessionFailureState
import com.aozijx.passly.domain.access.port.DatabaseSessionRecovery
import com.aozijx.passly.domain.access.port.DatabaseSessionRetryResult
import com.aozijx.passly.domain.access.port.SecureSessionAccessState
import com.aozijx.passly.domain.access.port.SessionLockController
import com.aozijx.passly.domain.settings.model.AppearanceSettings
import com.aozijx.passly.domain.settings.model.InterfaceSettings
import com.aozijx.passly.domain.settings.model.SecuritySettings
import com.aozijx.passly.domain.settings.model.ThemeMode
import com.aozijx.passly.domain.settings.model.AppLanguage
import com.aozijx.passly.domain.settings.model.FontFamilyMode
import com.aozijx.passly.domain.settings.port.AppearanceSettingsRepository
import com.aozijx.passly.domain.settings.port.InterfaceSettingsRepository
import com.aozijx.passly.domain.settings.port.SecuritySettingsSource
import com.aozijx.passly.testing.MainDispatcherRule
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AppShellViewModelTest {
    @get:Rule val main = MainDispatcherRule(StandardTestDispatcher())

    @Test
    fun `retry reserves state before dispatch and rejects duplicate calls`() = runTest {
        val release = CompletableDeferred<DatabaseSessionRetryResult>()
        var calls = 0
        val fixture = Fixture { calls++; release.await() }
        runCurrent()
        fixture.model.onAction(AppShellUiAction.RetryDatabaseSession)
        val immediate = fixture.phase()
        fixture.model.onAction(AppShellUiAction.RetryDatabaseSession)
        runCurrent()
        val startedCalls = calls
        release.complete(DatabaseSessionRetryResult.Unavailable)
        runCurrent()
        fixture.close()
        assertEquals(DatabaseRetryPhase.Retrying, immediate)
        assertEquals(1, startedCalls)
    }

    @Test
    fun `new authoritative failure cannot release an active retry slot`() = runTest {
        val release = CompletableDeferred<DatabaseSessionRetryResult>()
        var calls = 0
        val fixture = Fixture { calls++; release.await() }
        runCurrent()
        fixture.model.onAction(AppShellUiAction.RetryDatabaseSession)
        runCurrent()
        fixture.databaseFailure.value = IllegalStateException("new synthetic failure")
        runCurrent()
        val duringRetry = fixture.phase()
        fixture.model.onAction(AppShellUiAction.RetryDatabaseSession)
        runCurrent()
        val startedCalls = calls
        release.complete(DatabaseSessionRetryResult.Unavailable)
        runCurrent()
        fixture.close()
        assertEquals(DatabaseRetryPhase.Retrying, duringRetry)
        assertEquals(1, startedCalls)
    }

    @Test
    fun `cancelled retry returns to idle without failure feedback`() = runTest {
        val fixture = Fixture { throw CancellationException() }
        val effects = mutableListOf<AppShellEffect>()
        backgroundScope.launch { fixture.model.effects.toList(effects) }
        runCurrent()
        fixture.model.onAction(AppShellUiAction.RetryDatabaseSession)
        runCurrent()
        val phase = fixture.phase()
        fixture.close()
        assertEquals(DatabaseRetryPhase.Idle, phase)
        assertEquals(emptyList<AppShellEffect>(), effects)
    }

    @Test
    fun `availability follows failure source and retry is ignored once available`() = runTest {
        var calls = 0
        val fixture = Fixture { calls++; DatabaseSessionRetryResult.Ready }
        runCurrent()
        fixture.model.onAction(AppShellUiAction.RetryDatabaseSession)
        runCurrent()
        assertTrue(fixture.model.uiState.value.databaseState is AppShellDatabaseState.Failed)
        fixture.databaseFailure.value = null
        runCurrent()
        assertEquals(AppShellDatabaseState.Available, fixture.model.uiState.value.databaseState)
        fixture.model.onAction(AppShellUiAction.RetryDatabaseSession)
        runCurrent()
        fixture.close()
        assertEquals(1, calls)
    }

    @Test
    fun `authoritative recovery discards late failure from the previous retry`() = runTest {
        val release = CompletableDeferred<DatabaseSessionRetryResult>()
        val fixture = Fixture { release.await() }
        val effects = mutableListOf<AppShellEffect>()
        backgroundScope.launch { fixture.model.effects.toList(effects) }
        runCurrent()
        fixture.model.onAction(AppShellUiAction.RetryDatabaseSession)
        runCurrent()
        fixture.databaseFailure.value = null
        runCurrent()
        release.complete(DatabaseSessionRetryResult.Failed(IllegalStateException("stale failure")))
        runCurrent()
        assertEquals(AppShellDatabaseState.Available, fixture.model.uiState.value.databaseState)
        assertEquals(emptyList<AppShellEffect>(), effects)
        fixture.close()
    }

    @Test
    fun `cleared model rejects late recovery feedback`() = runTest {
        val release = CompletableDeferred<Unit>()
        val fixture = Fixture {
            withContext(NonCancellable) { release.await() }
            DatabaseSessionRetryResult.Failed(IllegalStateException("late synthetic failure"))
        }
        val effects = mutableListOf<AppShellEffect>()
        backgroundScope.launch { fixture.model.effects.toList(effects) }
        runCurrent()
        fixture.model.onAction(AppShellUiAction.RetryDatabaseSession)
        runCurrent()
        fixture.close()
        release.complete(Unit)
        runCurrent()
        assertEquals(emptyList<AppShellEffect>(), effects)
    }

    private class Fixture(retry: suspend () -> DatabaseSessionRetryResult) : AppearanceSettingsRepository,
        InterfaceSettingsRepository, SecuritySettingsSource, SecureSessionAccessState,
        SessionLockController, DatabaseSessionFailureState {
        override val appearance = emptyFlow<AppearanceSettings>()
        override val interfaceSettings = emptyFlow<InterfaceSettings>()
        override val security = emptyFlow<SecuritySettings>()
        override val authenticationState = MutableStateFlow<AuthenticationState>(AuthenticationState.Locked)
        override val databaseFailure = MutableStateFlow<Throwable?>(IllegalStateException("synthetic failure"))
        val model = AppShellViewModel(this, this, this, this, this, this, DatabaseSessionRecovery { retry() })
        fun phase() = (model.uiState.value.databaseState as AppShellDatabaseState.Failed).retryPhase
        fun close() { ViewModelStore().apply { put("shell", model); clear() } }
        override fun isUnlocked() = false
        override suspend fun lock(request: SessionLockRequest) = Unit
        override suspend fun setThemeMode(mode: ThemeMode) = Unit
        override suspend fun setDynamicColor(enabled: Boolean) = Unit
        override suspend fun setThemeKey(key: String) = Unit
        override suspend fun setLanguage(language: AppLanguage) = Unit
        override suspend fun setFontFamily(mode: FontFamilyMode) = Unit
        override suspend fun setHideSystemBars(enabled: Boolean) = Unit
        override suspend fun setTopBarCollapsible(enabled: Boolean) = Unit
        override suspend fun setQuickFilterBarCollapsible(enabled: Boolean) = Unit
        override suspend fun setAppCornerRadius(radiusDp: Float) = Unit
    }
}
