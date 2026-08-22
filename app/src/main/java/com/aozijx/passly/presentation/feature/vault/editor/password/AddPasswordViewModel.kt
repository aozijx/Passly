package com.aozijx.passly.presentation.feature.vault.editor.password

import com.aozijx.passly.domain.access.port.SecureSessionAccessState
import com.aozijx.passly.domain.entry.port.EntryCommandRepository
import com.aozijx.passly.presentation.feature.vault.editor.common.CreateEntryViewModel
import com.aozijx.passly.feature.vault.editor.password.PasswordEntryFactory
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class AddPasswordViewModel @Inject constructor(
    entryCommandRepository: EntryCommandRepository,
    secureSessionAccessState: SecureSessionAccessState
) : CreateEntryViewModel<AddPasswordFormState>(
    initialForm = AddPasswordFormState(),
    entryCommandRepository = entryCommandRepository,
    secureSessionAccessState = secureSessionAccessState,
    isFormValid = AddPasswordFormState::isValid,
    createEntry = { PasswordEntryFactory.create(it) }
) {

    fun onAction(action: AddPasswordAction) {
        when (action) {
            is AddPasswordAction.TitleChanged -> mutateForm { it.copy(title = action.value) }
            is AddPasswordAction.UsernameChanged -> mutateForm {
                it.copy(username = action.value)
            }
            is AddPasswordAction.PasswordChanged -> mutateForm {
                it.copy(password = action.value)
            }
            is AddPasswordAction.PasswordVisibilityChanged -> mutateForm {
                it.copy(isPasswordVisible = action.visible)
            }
            is AddPasswordAction.WebsiteChanged -> mutateForm { it.copy(website = action.value) }
            is AddPasswordAction.NotesChanged -> mutateForm { it.copy(notes = action.value) }
            is AddPasswordAction.TagsChanged -> mutateForm { it.copy(tags = action.value) }
            AddPasswordAction.Save -> saveEntry()
        }
    }
}
