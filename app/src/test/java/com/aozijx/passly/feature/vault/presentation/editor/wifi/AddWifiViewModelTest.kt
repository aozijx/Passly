package com.aozijx.passly.feature.vault.presentation.editor.wifi

import com.aozijx.passly.core.error.model.Conflict
import com.aozijx.passly.core.error.result.AppResult
import com.aozijx.passly.domain.access.model.AuthenticationState
import com.aozijx.passly.domain.access.port.SecureSessionAccessState
import com.aozijx.passly.domain.entry.model.Entry
import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.entry.model.EntryUpdate
import com.aozijx.passly.domain.entry.model.EntryVersion
import com.aozijx.passly.domain.entry.model.FieldKey
import com.aozijx.passly.domain.entry.model.credential.WifiSecurityType
import com.aozijx.passly.domain.entry.port.EntryCommandRepository
import com.aozijx.passly.feature.vault.entry.CreateEntryUseCase
import com.aozijx.passly.domain.entry.policy.EntryDraftMaterializer
import com.aozijx.passly.domain.entry.policy.WifiQrPayload
import com.aozijx.passly.domain.entry.policy.WifiQrPayloadFormatter
import com.aozijx.passly.domain.entry.policy.WifiQrFormatResult
import com.aozijx.passly.domain.entry.policy.WifiQrPayloadParser
import com.aozijx.passly.domain.entry.policy.WifiQrParseResult
import com.aozijx.passly.testing.testLogger
import com.aozijx.passly.feature.vault.presentation.editor.EditorSaveEffect
import com.aozijx.passly.testing.MainDispatcherRule
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AddWifiViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `accepted scan preserves notes tags and exact network fields`() {
        val model = viewModel(RecordingRepository())
        model.onAction(AddWifiAction.NotesChanged("Keep notes"))
        model.onAction(AddWifiAction.TagsChanged("home,network"))
        model.onAction(AddWifiAction.PasswordVisibilityChanged(true))
        val scanned = WifiQrPayload(" Office ", " password ", WifiSecurityType.WpaPersonal, true)

        assertTrue(model.tryApplyScannedWifi(scanned))

        val form = model.uiState.value.form
        assertEquals(scanned.ssid, form.ssid)
        assertEquals(scanned.password, form.password)
        assertEquals(scanned.securityType, form.securityType)
        assertEquals(scanned.isHidden, form.isHidden)
        assertFalse(form.isPasswordVisible)
        assertEquals("Keep notes", form.notes)
        assertEquals("home,network", form.tags)
    }

    @Test
    fun `scan during save is rejected without changing submitted credentials`() = runTest {
        val release = CompletableDeferred<Unit>()
        val created = mutableListOf<Entry>()
        val model = viewModel(RecordingRepository { entry ->
            created += entry
            release.await()
            AppResult.Success(entry.id)
        })
        enterValidWpa(model)
        model.onAction(AddWifiAction.Save)
        runCurrent()

        assertFalse(model.tryApplyScannedWifi(WifiQrPayload("Other", "other-password", WifiSecurityType.WpaPersonal, true)))
        assertEquals("Office", model.uiState.value.form.ssid)
        assertEquals("password", model.uiState.value.form.password)
        assertEquals("Office", created.single().secret.wifi?.ssid)
        release.complete(Unit)
        advanceUntilIdle()
    }

    @Test
    fun `locked and destroyed editors reject scans without retaining payload`() = runTest {
        val session = MutableSession()
        val model = viewModel(RecordingRepository(), session)
        val scanned = WifiQrPayload("Office", "password", WifiSecurityType.WpaPersonal, false)
        session.lock()
        // Reject immediately, even before the authentication collector runs.
        assertFalse(model.tryApplyScannedWifi(scanned))
        assertEquals("", model.uiState.value.form.password)
        session.unlock()
        runCurrent()
        assertTrue(model.tryApplyScannedWifi(scanned))
        val onCleared = model.javaClass.getDeclaredMethod("onCleared")
        onCleared.isAccessible = true
        onCleared.invoke(model)
        assertFalse(model.tryApplyScannedWifi(scanned))
        assertEquals("", model.uiState.value.form.ssid)
        assertEquals("", model.uiState.value.form.password)
    }

    @Test
    fun `old save cannot clear a new form after locking and unlocking`() = runTest {
        val release = CompletableDeferred<Unit>()
        val repository = RecordingRepository { entry ->
            withContext(NonCancellable) { release.await() }
            AppResult.Success(entry.id)
        }
        val session = MutableSession()
        val model = viewModel(repository, session)
        enterValidWpa(model)
        model.onAction(AddWifiAction.Save)
        runCurrent()
        session.lock()
        runCurrent()
        session.unlock()
        runCurrent()
        model.onAction(AddWifiAction.SsidChanged("New form"))
        model.onAction(AddWifiAction.PasswordChanged("new-password"))
        release.complete(Unit)
        advanceUntilIdle()
        assertEquals("New form", model.uiState.value.form.ssid)
        assertEquals("new-password", model.uiState.value.form.password)
    }

    @Test
    fun `late input actions cannot refill sensitive form content while locked`() = runTest {
        val session = MutableSession()
        val model = viewModel(RecordingRepository(), session)
        session.lock()
        runCurrent()
        model.onAction(AddWifiAction.PasswordChanged("late-password"))
        assertEquals("", model.uiState.value.form.password)
    }

    @Test
    fun `manual creation preserves the exact network name through materialization`() = runTest {
        val created = mutableListOf<Entry>()
        val repository = RecordingRepository { entry -> created += entry; AppResult.Success(entry.id) }
        val model = viewModel(repository)
        model.onAction(AddWifiAction.SsidChanged("  Office  "))
        model.onAction(AddWifiAction.PasswordChanged("password"))
        model.onAction(AddWifiAction.Save)
        advanceUntilIdle()
        assertEquals("  Office  ", created.single().title)
        assertEquals("  Office  ", created.single().secret.wifi?.ssid)
    }

    @Test
    fun `scanned network survives creation and qr export without changing its name`() = runTest {
        val original = WifiQrPayload("  办公;网\\:  ", "  password  ", WifiSecurityType.WpaPersonal, true)
        val initialQr = WifiQrPayloadFormatter.format(original) as WifiQrFormatResult.Formatted
        val scanned = WifiQrPayloadParser.parse(initialQr.content) as WifiQrParseResult.Parsed
        val created = mutableListOf<Entry>()
        val repository = RecordingRepository { entry -> created += entry; AppResult.Success(entry.id) }
        val model = viewModel(repository)
        model.initialize(AddWifiInitialContent.Scanned(scanned.payload))
        model.onAction(AddWifiAction.Save)
        advanceUntilIdle()
        val wifi = requireNotNull(created.single().secret.wifi)
        val exported = WifiQrPayloadFormatter.format(WifiQrPayload(
            wifi.ssid, wifi.password.orEmpty(), wifi.securityType, wifi.isHidden,
        )) as WifiQrFormatResult.Formatted
        val parsed = WifiQrPayloadParser.parse(exported.content) as WifiQrParseResult.Parsed
        assertEquals(original, parsed.payload)
    }

    @Test
    fun `preserving network whitespace does not permit an entirely blank name`() = runTest {
        val repository = RecordingRepository()
        val model = viewModel(repository)
        model.onAction(AddWifiAction.SsidChanged(" \t "))
        model.onAction(AddWifiAction.SecurityTypeChanged(WifiSecurityType.Open))
        assertFalse(model.uiState.value.canSave)
        model.onAction(AddWifiAction.Save)
        advanceUntilIdle()
        assertEquals(0, repository.createCalls)
    }

    @Test
    fun `blank required fields disable save without showing errors`() {
        val state = viewModel(RecordingRepository()).uiState.value

        assertFalse(state.canSave)
        assertTrue(state.fieldErrors.isEmpty())
    }

    @Test
    fun `domain wifi validation drives save availability and field errors`() {
        val viewModel = viewModel(RecordingRepository())

        viewModel.onAction(AddWifiAction.SsidChanged("Office"))
        viewModel.onAction(AddWifiAction.PasswordChanged("short"))

        assertFalse(viewModel.uiState.value.canSave)
        assertTrue(FieldKey.PASSWORD in viewModel.uiState.value.fieldErrors)

        viewModel.onAction(AddWifiAction.PasswordChanged("password"))

        assertTrue(viewModel.uiState.value.canSave)
        assertTrue(viewModel.uiState.value.fieldErrors.isEmpty())
    }

    @Test
    fun `duplicate save is suppressed while persistence is in flight`() = runTest {
        val release = CompletableDeferred<Unit>()
        val repository = RecordingRepository { entry ->
            release.await()
            AppResult.Success(entry.id)
        }
        val viewModel = viewModel(repository)
        enterValidWpa(viewModel)

        viewModel.onAction(AddWifiAction.Save)
        viewModel.onAction(AddWifiAction.Save)
        runCurrent()

        assertEquals(1, repository.createCalls)
        assertEquals(AddWifiSavePhase.Saving, viewModel.uiState.value.savePhase)

        release.complete(Unit)
        advanceUntilIdle()
        assertEquals(1, repository.createCalls)
    }

    @Test
    fun `successful save clears sensitive state and emits saved effect`() = runTest {
        val repository = RecordingRepository { entry -> AppResult.Success(entry.id) }
        val viewModel = viewModel(repository)
        enterValidWpa(viewModel)

        viewModel.onAction(AddWifiAction.Save)
        advanceUntilIdle()

        assertEquals(EditorSaveEffect.Saved, viewModel.effects.first())
        assertEquals("", viewModel.uiState.value.form.password)
        assertEquals("", viewModel.uiState.value.form.ssid)
        assertFalse(viewModel.uiState.value.canSave)
    }

    @Test
    fun `failed save restores the form and emits a failure effect`() = runTest {
        val repository = RecordingRepository {
            AppResult.Failure(Conflict(errorId = "conflict"))
        }
        val viewModel = viewModel(repository)
        enterValidWpa(viewModel)

        viewModel.onAction(AddWifiAction.Save)
        advanceUntilIdle()

        assertTrue(viewModel.effects.first() is EditorSaveEffect.Failed)
        assertEquals("password", viewModel.uiState.value.form.password)
        assertTrue(viewModel.uiState.value.canSave)
        assertEquals(AddWifiSavePhase.Idle, viewModel.uiState.value.savePhase)
    }

    @Test
    fun `authentication loss and clearing wipe sensitive wifi state`() = runTest {
        val session = MutableSession()
        val viewModel = viewModel(RecordingRepository(), session)
        enterValidWpa(viewModel)

        session.lock()
        advanceUntilIdle()

        assertEquals("", viewModel.uiState.value.form.password)
        assertEquals("", viewModel.uiState.value.form.ssid)

        enterValidWpa(viewModel)
        val onCleared = viewModel.javaClass.getDeclaredMethod("onCleared")
        onCleared.isAccessible = true
        onCleared.invoke(viewModel)

        assertEquals("", viewModel.uiState.value.form.password)
        assertEquals("", viewModel.uiState.value.form.ssid)
    }

    @Test
    fun `scanned initial content is applied only once`() {
        val viewModel = viewModel(RecordingRepository())
        val scanned = AddWifiInitialContent.Scanned(
            com.aozijx.passly.domain.entry.policy.WifiQrPayload(
                "Office",
                "password",
                WifiSecurityType.WpaPersonal,
                false,
            ),
        )

        viewModel.initialize(scanned)
        viewModel.onAction(AddWifiAction.SsidChanged("Edited"))
        viewModel.initialize(AddWifiInitialContent.Blank)

        assertEquals("Edited", viewModel.uiState.value.form.ssid)
        assertEquals("password", viewModel.uiState.value.form.password)
    }

    private fun enterValidWpa(viewModel: AddWifiViewModel) {
        viewModel.onAction(AddWifiAction.SsidChanged("Office"))
        viewModel.onAction(AddWifiAction.SecurityTypeChanged(WifiSecurityType.WpaPersonal))
        viewModel.onAction(AddWifiAction.PasswordChanged("password"))
    }

    private fun viewModel(
        repository: EntryCommandRepository,
        session: SecureSessionAccessState = AuthenticatedSession,
    ) = AddWifiViewModel(
        createEntryUseCase = CreateEntryUseCase(repository, session, EntryDraftMaterializer(), testLogger()),
        secureSessionAccessState = session,
    )

    private object AuthenticatedSession : SecureSessionAccessState {
        override val authenticationState: StateFlow<AuthenticationState> =
            MutableStateFlow(AuthenticationState.Authenticated(1L))
        override fun isUnlocked() = true
    }

    private class MutableSession : SecureSessionAccessState {
        private val state = MutableStateFlow<AuthenticationState>(AuthenticationState.Authenticated(1L))
        override val authenticationState: StateFlow<AuthenticationState> = state
        override fun isUnlocked() = state.value is AuthenticationState.Authenticated
        fun lock() {
            state.value = AuthenticationState.Locked
        }
        fun unlock() {
            state.value = AuthenticationState.Authenticated(2L)
        }
    }

    private class RecordingRepository(
        private val create: suspend (Entry) -> AppResult<EntryId> = { AppResult.Success(it.id) },
    ) : EntryCommandRepository {
        var createCalls = 0

        override suspend fun createEntry(entry: Entry): AppResult<EntryId> {
            createCalls++
            return create(entry)
        }

        override suspend fun updateEntry(id: EntryId, expectedVersion: EntryVersion, changes: EntryUpdate) =
            AppResult.Success(Unit)
        override suspend fun moveToTrash(id: EntryId, expectedVersion: EntryVersion) = AppResult.Success(Unit)
        override suspend fun restoreEntry(id: EntryId, expectedVersion: EntryVersion) = AppResult.Success(Unit)
        override suspend fun deletePermanently(id: EntryId, expectedVersion: EntryVersion) = AppResult.Success(Unit)
        override suspend fun emptyTrash() = AppResult.Success(0)
    }
}
