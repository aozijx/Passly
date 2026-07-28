package com.aozijx.passly.feature.vault.editor.password

import com.aozijx.passly.domain.entry.repository.EntryCommandRepository
import com.aozijx.passly.feature.vault.editor.common.CreateEntryViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class AddPasswordViewModel @Inject constructor(
    entryCommandRepository: EntryCommandRepository
) : CreateEntryViewModel<AddPasswordFormState>(
    initialForm = AddPasswordFormState(),
    entryCommandRepository = entryCommandRepository,
    isFormValid = AddPasswordFormState::isValid,
    createEntry = { PasswordEntryFactory.create(it) }
) {

    fun updateTitle(value: String) = mutateForm { it.copy(title = value) }
    fun updateUsername(value: String) = mutateForm { it.copy(username = value) }
    fun updatePassword(value: String) = mutateForm { it.copy(password = value) }
    fun updateWebsite(value: String) = mutateForm { it.copy(website = value) }
    fun updateNotes(value: String) = mutateForm { it.copy(notes = value) }
    fun setPasswordVisible(visible: Boolean) =
        mutateForm { it.copy(isPasswordVisible = visible) }
}
