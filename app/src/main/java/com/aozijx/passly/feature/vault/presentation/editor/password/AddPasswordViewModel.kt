package com.aozijx.passly.feature.vault.presentation.editor.password

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aozijx.passly.core.platform.packageinfo.InstalledAppDirectory
import com.aozijx.passly.domain.access.port.SecureSessionAccessState
import com.aozijx.passly.feature.vault.entry.CreateEntryUseCase
import com.aozijx.passly.feature.vault.detail.EditDetailEntryUseCase
import com.aozijx.passly.feature.vault.detail.EditableEntryLoadResult
import com.aozijx.passly.feature.vault.detail.LoadEditableEntryUseCase
import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.policy.EntryEdit
import com.aozijx.passly.core.error.result.AppResult
import com.aozijx.passly.feature.vault.presentation.editor.EditorSaveSession
import com.aozijx.passly.feature.vault.presentation.editor.EditorLoadPhase
import com.aozijx.passly.feature.vault.presentation.common.AppPackagePickerItemUiModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddPasswordViewModel @Inject internal constructor(
    createEntryUseCase: CreateEntryUseCase,
    private val secureSessionAccessState: SecureSessionAccessState,
    private val installedAppDirectory: InstalledAppDirectory,
    private val loadEditableEntry: LoadEditableEntryUseCase,
    private val editDetailEntry: EditDetailEntryUseCase,
) : ViewModel() {
    private var associatedAppsLoadJob: Job? = null
    private var editLoadJob: Job? = null
    private var editingEntryId: EntryId? = null
    private val _uiState = MutableStateFlow(AddPasswordUiState())
    val uiState = _uiState.asStateFlow()
    private val saveSession = EditorSaveSession(
        scope = viewModelScope,
        access = secureSessionAccessState,
        create = { draft ->
            val entryId = editingEntryId
            if (entryId == null) createEntryUseCase(draft)
            else editDetailEntry.edit(entryId, EntryEdit.ReplaceForm(draft))
        },
        onSaving = { _uiState.value = _uiState.value.copy(savePhase = AddPasswordSavePhase.Saving) },
        onIdle = {
            _uiState.value = _uiState.value.copy(savePhase = AddPasswordSavePhase.Idle)
        },
        onClear = ::clearSensitiveContent,
    )
    internal val effects = saveSession.effects

    fun loadExistingEntry(rawEntryId: String) {
        if (editingEntryId != null) return
        val entryId = runCatching { EntryId(rawEntryId) }.getOrNull() ?: run {
            _uiState.value = _uiState.value.copy(loadPhase = EditorLoadPhase.Failed, loadFailed = true)
            return
        }
        editingEntryId = entryId
        if (!saveSession.canEdit()) {
            _uiState.value = _uiState.value.copy(loadPhase = EditorLoadPhase.Failed, loadFailed = true)
            return
        }
        val authentication = secureSessionAccessState.authenticationState.value
        _uiState.value = _uiState.value.copy(loadPhase = EditorLoadPhase.Loading)
        editLoadJob = viewModelScope.launch {
            when (val result = loadEditableEntry(entryId)) {
                is EditableEntryLoadResult.Loaded -> {
                    if (result.entry.type != EntryType.LOGIN ||
                        result.entry.id != entryId || authentication !== result.authentication ||
                        secureSessionAccessState.authenticationState.value !== result.authentication ||
                        !secureSessionAccessState.hasFullSecureSessionAccess()
                    ) {
                        failEditLoad()
                    } else {
                        _uiState.value = _uiState.value.copy(
                            form = _uiState.value.form.fromEntry(result.entry),
                            loadPhase = EditorLoadPhase.Ready,
                        )
                    }
                }
                else -> failEditLoad()
            }
        }
    }

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
        if (!saveSession.canEdit()) return
        if (_uiState.value.availableAppsLoaded || associatedAppsLoadJob?.isActive == true) return
        val authentication = secureSessionAccessState.authenticationState.value
        associatedAppsLoadJob = viewModelScope.launch {
            if (!secureSessionAccessState.hasFullSecureSessionAccess() ||
                secureSessionAccessState.authenticationState.value !== authentication) return@launch
            try {
                val apps = installedAppDirectory.launchableApps().map { metadata ->
                    AppPackagePickerItemUiModel(metadata.label, metadata.packageName)
                }
                coroutineContext.ensureActive()
                if (!secureSessionAccessState.hasFullSecureSessionAccess() ||
                    secureSessionAccessState.authenticationState.value !== authentication) return@launch
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
        if (!saveSession.canEdit() || !canEditForm()) return
        val current = _uiState.value
        val form = transform(current.form)
        _uiState.value = current.copy(form = form)
    }

    private fun save() {
        val current = _uiState.value
        if (!canEditForm()) return
        if (!current.canSave) return
        saveSession.save { current.form.toEntryDraft() }
    }

    private fun clearSensitiveContent() {
        editLoadJob?.cancel()
        editLoadJob = null
        associatedAppsLoadJob?.cancel()
        associatedAppsLoadJob = null
        _uiState.value = AddPasswordUiState(
            loadPhase = if (editingEntryId == null) EditorLoadPhase.NotRequested else EditorLoadPhase.Failed,
        )
    }

    private fun canEditForm() = editingEntryId == null || _uiState.value.loadPhase == EditorLoadPhase.Ready

    private fun failEditLoad() {
        _uiState.value = _uiState.value.copy(
            form = AddPasswordFormState(), loadPhase = EditorLoadPhase.Failed, loadFailed = true,
        )
    }

    override fun onCleared() {
        saveSession.close()
    }
}
