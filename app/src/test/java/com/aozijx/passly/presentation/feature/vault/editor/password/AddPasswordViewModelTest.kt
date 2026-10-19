package com.aozijx.passly.presentation.feature.vault.editor.password

import com.aozijx.passly.core.error.result.AppResult
import com.aozijx.passly.core.platform.packageinfo.InstalledAppDirectory
import com.aozijx.passly.core.platform.packageinfo.InstalledAppMetadata
import com.aozijx.passly.domain.access.model.AuthenticationState
import com.aozijx.passly.domain.access.port.SecureSessionAccessState
import com.aozijx.passly.domain.entry.model.Entry
import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.entry.model.EntryUpdate
import com.aozijx.passly.domain.entry.model.EntryVersion
import com.aozijx.passly.domain.entry.port.EntryCommandRepository
import com.aozijx.passly.feature.vault.entry.CreateEntryUseCase
import com.aozijx.passly.feature.vault.entry.EntryDraftMaterializer
import com.aozijx.passly.testing.MainDispatcherRule
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.Rule

@OptIn(ExperimentalCoroutinesApi::class)
class AddPasswordViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun formAllowsPasswordOnlyLoginAndPreservesPasswordWhitespace() {
        val viewModel = viewModel()

        viewModel.onAction(AddPasswordAction.TitleChanged("Mail"))
        viewModel.onAction(AddPasswordAction.PasswordChanged(" secret "))

        val state: AddPasswordUiState = viewModel.uiState.value
        assertTrue(state.canSave)
        assertEquals(AddPasswordSavePhase.Idle, state.savePhase)
        assertEquals("", state.form.username)
        assertEquals(" secret ", state.form.password)
    }

    @Test
    fun saveCapabilityIsDerivedFromFormAndSavePhase() {
        val form = AddPasswordFormState(title = "Mail", password = "secret")

        assertTrue(AddPasswordUiState(form = form).canSave)
        assertTrue(
            !AddPasswordUiState(
                form = form,
                savePhase = AddPasswordSavePhase.Saving,
            ).canSave,
        )
    }

    @Test
    fun onClearedWipesPasswordContent() {
        val viewModel = viewModel()
        viewModel.onAction(AddPasswordAction.TitleChanged("Mail"))
        viewModel.onAction(AddPasswordAction.PasswordChanged("secret"))

        val onCleared = viewModel.javaClass.getDeclaredMethod("onCleared")
        onCleared.isAccessible = true
        onCleared.invoke(viewModel)

        assertEquals("", viewModel.uiState.value.form.password)
        assertEquals("", viewModel.uiState.value.form.title)
        assertTrue(!viewModel.uiState.value.canSave)
    }

    @Test
    fun `loading and selecting an associated app updates the form`() = runTest {
        val viewModel = viewModel(
            apps = listOf(InstalledAppMetadata("Mail", "com.example.mail")),
        )

        viewModel.onAction(AddPasswordAction.LoadAssociatedApps)
        advanceUntilIdle()
        viewModel.onAction(AddPasswordAction.AssociatedAppSelected("com.example.mail"))

        assertEquals("com.example.mail", viewModel.uiState.value.form.associatedAppPackage)
        assertEquals("Mail", viewModel.uiState.value.availableApps.single().label)
    }

    @Test
    fun sessionLockWipesPasswordContent() = runTest {
        val session = MutableSession()
        val viewModel = viewModel(session = session)
        viewModel.onAction(AddPasswordAction.TitleChanged("Mail"))
        viewModel.onAction(AddPasswordAction.PasswordChanged("secret"))

        session.lock()

        assertEquals("", viewModel.uiState.value.form.password)
        assertTrue(!viewModel.uiState.value.canSave)
    }

    private fun createEntryUseCase(session: SecureSessionAccessState) = CreateEntryUseCase(
        entryCommandRepository = NoOpEntryCommandRepository,
        secureSessionAccessState = session,
        materializer = EntryDraftMaterializer(),
    )

    private fun viewModel(
        session: SecureSessionAccessState = AuthenticatedSession,
        apps: List<InstalledAppMetadata> = emptyList(),
    ) = AddPasswordViewModel(
        createEntryUseCase = createEntryUseCase(session),
        secureSessionAccessState = session,
        installedAppDirectory = FakeInstalledAppDirectory(apps),
    )

    private class FakeInstalledAppDirectory(
        private val apps: List<InstalledAppMetadata>,
    ) : InstalledAppDirectory {
        override suspend fun metadataFor(packageName: String) =
            apps.firstOrNull { it.packageName == packageName }

        override suspend fun launchableApps() = apps
    }

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
    }

    private object NoOpEntryCommandRepository : EntryCommandRepository {
        override suspend fun createEntry(entry: Entry) = AppResult.Success(entry.id)
        override suspend fun updateEntry(id: EntryId, expectedVersion: EntryVersion, changes: EntryUpdate) =
            AppResult.Success(Unit)
        override suspend fun moveToTrash(id: EntryId, expectedVersion: EntryVersion) = AppResult.Success(Unit)
        override suspend fun restoreEntry(id: EntryId, expectedVersion: EntryVersion) = AppResult.Success(Unit)
        override suspend fun deletePermanently(id: EntryId, expectedVersion: EntryVersion) = AppResult.Success(Unit)
        override suspend fun emptyTrash() = AppResult.Success(0)
    }
}
