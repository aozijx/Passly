package com.aozijx.passly.presentation.feature.vault.editor.password

import com.aozijx.passly.core.error.result.AppResult
import com.aozijx.passly.domain.access.model.AuthenticationState
import com.aozijx.passly.domain.access.port.SecureSessionAccessState
import com.aozijx.passly.domain.entry.model.Entry
import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.entry.model.EntryUpdate
import com.aozijx.passly.domain.entry.model.EntryVersion
import com.aozijx.passly.domain.entry.port.EntryCommandRepository
import com.aozijx.passly.feature.vault.entry.CreateEntryUseCase
import com.aozijx.passly.feature.vault.entry.EntryDraftMaterializer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AddPasswordViewModelTest {
    @Test
    fun formAllowsPasswordOnlyLoginAndPreservesPasswordWhitespace() {
        val viewModel = AddPasswordViewModel(createEntryUseCase())

        viewModel.onAction(AddPasswordAction.TitleChanged("Mail"))
        viewModel.onAction(AddPasswordAction.PasswordChanged(" secret "))

        assertTrue(viewModel.uiState.value.canSave)
        assertEquals("", viewModel.uiState.value.form.username)
        assertEquals(" secret ", viewModel.uiState.value.form.password)
    }

    @Test
    fun onClearedWipesPasswordContent() {
        val viewModel = AddPasswordViewModel(createEntryUseCase())
        viewModel.onAction(AddPasswordAction.TitleChanged("Mail"))
        viewModel.onAction(AddPasswordAction.PasswordChanged("secret"))

        val onCleared = requireNotNull(viewModel.javaClass.superclass).getDeclaredMethod("onCleared")
        onCleared.isAccessible = true
        onCleared.invoke(viewModel)

        assertEquals("", viewModel.uiState.value.form.password)
        assertEquals("", viewModel.uiState.value.form.title)
        assertTrue(!viewModel.uiState.value.canSave)
    }

    private fun createEntryUseCase() = CreateEntryUseCase(
        entryCommandRepository = NoOpEntryCommandRepository,
        secureSessionAccessState = AuthenticatedSession,
        materializer = EntryDraftMaterializer(),
    )

    private object AuthenticatedSession : SecureSessionAccessState {
        override val authenticationState: StateFlow<AuthenticationState> =
            MutableStateFlow(AuthenticationState.Authenticated(1L))
        override fun isUnlocked() = true
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
