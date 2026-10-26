package com.aozijx.passly.feature.vault.presentation.editor.password

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aozijx.passly.core.platform.packageinfo.InstalledAppDirectory
import com.aozijx.passly.domain.access.port.SecureSessionAccessState
import com.aozijx.passly.feature.vault.entry.CreateEntryUseCase
import com.aozijx.passly.feature.vault.presentation.editor.EditorSaveSession
import com.aozijx.passly.presentation.shared.components.AppPackagePickerItemUiModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddPasswordViewModel @Inject constructor(
    createEntryUseCase: CreateEntryUseCase,
    secureSessionAccessState: SecureSessionAccessState,
    private val installedAppDirectory: InstalledAppDirectory,
) : ViewModel() {
    private var associatedAppsLoadJob: Job? = null
    private val _uiState = MutableStateFlow(AddPasswordUiState())
    val uiState = _uiState.asStateFlow()
    private val saveSession = EditorSaveSession(
        scope = viewModelScope,
        access = secureSessionAccessState,
        create = createEntryUseCase::invoke,
        onSaving = { _uiState.value = _uiState.value.copy(savePhase = AddPasswordSavePhase.Saving) },
        onIdle = {
            _uiState.value = _uiState.value.copy(savePhase = AddPasswordSavePhase.Idle)
        },
        onClear = ::clearSensitiveContent,
    )
    internal val effects = saveSession.effects

    fun onAction(action: AddPasswordAction) {
        when (action) {
            is AddPasswordAction.TitleChanged -> mutateForm { it.copy(title = action.value) }
            is AddPasswordAction.UsernameChanged -> mutateForm { it.copy(username = action.value) }
            is AddPasswordAction.PasswordChanged -> mutateForm { it.copy(password = action.value) }
            is AddPasswordAction.PasswordVisibilityChanged -> mutateForm { it.copy(isPasswordVisible = action.visible) }
            is AddPasswordAction.WebsiteUrlChanged -> mutateForm {
                it.copy(websiteUrl = action.value)
            }
            is AddPasswordAction.NotesChanged -> mutateForm { it.copy(notes = action.value) }
            is AddPasswordAction.TagsChanged -> mutateForm { it.copy(tags = action.value) }
            AddPasswordAction.LoadAssociatedApps -> loadAssociatedApps()
            is AddPasswordAction.AssociatedAppSelected -> mutateForm {
                it.copy(associatedAppPackage = action.packageName)
            }
            AddPasswordAction.Save -> save()
        }
    }

    private fun loadAssociatedApps() {
        if (_uiState.value.availableAppsLoaded || associatedAppsLoadJob?.isActive == true) return
        associatedAppsLoadJob = viewModelScope.launch {
            try {
                val apps = installedAppDirectory.launchableApps().map { metadata ->
                    AppPackagePickerItemUiModel(metadata.label, metadata.packageName)
                }
                _uiState.value = _uiState.value.copy(
                    availableApps = apps,
                    availableAppsLoaded = true,
                )
            } catch (error: CancellationException) {
                throw error
            } catch (_: Throwable) {
                // Keep the state retryable; reopening the picker triggers another load.
            }
        }
    }

    private fun mutateForm(transform: (AddPasswordFormState) -> AddPasswordFormState) {
        if (!saveSession.canEdit()) return
        val current = _uiState.value
        val form = transform(current.form)
        _uiState.value = current.copy(form = form)
    }

    private fun save() {
        val current = _uiState.value
        if (!current.canSave) return
        saveSession.save { current.form.toEntryDraft() }
    }

    private fun clearSensitiveContent() {
        associatedAppsLoadJob?.cancel()
        associatedAppsLoadJob = null
        _uiState.value = AddPasswordUiState()
    }

    override fun onCleared() {
        saveSession.close()
    }
}
