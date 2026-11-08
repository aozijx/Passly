package com.aozijx.passly.feature.vault.presentation.editor.password

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aozijx.passly.core.error.result.AppResult
import com.aozijx.passly.core.platform.packageinfo.InstalledAppDirectory
import com.aozijx.passly.domain.access.port.SecureSessionAccessState
import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.policy.EntryEdit
import com.aozijx.passly.domain.entry.port.EntryCreation
import com.aozijx.passly.feature.vault.entry.EditEntryUseCase
import com.aozijx.passly.feature.vault.entry.EntryEditorSeed
import com.aozijx.passly.feature.vault.entry.EntryEditorSeedConsumeResult
import com.aozijx.passly.feature.vault.presentation.common.AppPackagePickerItemUiModel
import com.aozijx.passly.feature.vault.presentation.common.AppPackagePickerUiState
import com.aozijx.passly.feature.vault.presentation.common.appPackagePickerReady
import com.aozijx.passly.feature.vault.presentation.common.search
import com.aozijx.passly.feature.vault.presentation.editor.EditorLoadPhase
import com.aozijx.passly.feature.vault.presentation.editor.EditorWriteSession
import com.aozijx.passly.feature.vault.presentation.editor.EditorWriteSessionClearReason
import com.aozijx.passly.feature.vault.presentation.editor.toEditorLoadPhase
import com.aozijx.passly.feature.vault.presentation.editor.matchesCurrentAccess
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class AddPasswordViewModel
    @Inject
    internal constructor(
        createEntryUseCase: EntryCreation,
        private val secureSessionAccessState: SecureSessionAccessState,
        private val installedAppDirectory: InstalledAppDirectory,
        private val editEntry: EditEntryUseCase,
    ) : ViewModel() {
        private var associatedAppsLoadJob: Job? = null
        private var editingEntryId: EntryId? = null
        private val _uiState = MutableStateFlow(AddPasswordUiState())
        val uiState = _uiState.asStateFlow()
        private val writeSession =
            EditorWriteSession(
                scope = viewModelScope,
                access = secureSessionAccessState,
                write = { draft ->
                    val entryId = editingEntryId
                    if (entryId == null) {
                        createEntryUseCase(draft)
                    } else {
                        editEntry.edit(entryId, EntryEdit.ReplaceForm(draft))
                    }
                },
                onSaving = { _uiState.value = _uiState.value.copy(savePhase = AddPasswordSavePhase.Saving) },
                onIdle = {
                    _uiState.value = _uiState.value.copy(savePhase = AddPasswordSavePhase.Idle)
                },
                onClear = ::clearSensitiveContent,
            )
        internal val effects = writeSession.effects

        fun loadExistingEntry(result: EntryEditorSeedConsumeResult) {
            if (editingEntryId != null) return
            val seed = (result as? EntryEditorSeedConsumeResult.Consumed)?.seed as? EntryEditorSeed.Editable
            if (seed == null || !seed.matchesCurrentAccess(EntryType.LOGIN, secureSessionAccessState) || !writeSession.canEdit()) {
                failEditLoad()
                return
            }
            editingEntryId = seed.entry.id
            _uiState.value =
                _uiState.value.copy(
                    form = _uiState.value.form.fromEntry(seed.entry),
                    loadPhase = EditorLoadPhase.Ready,
                )
        }

        fun onAction(action: AddPasswordAction) {
            when (action) {
                is AddPasswordAction.TitleChanged -> {
                    mutateForm { it.copy(title = action.value) }
                }

                is AddPasswordAction.UsernameChanged -> {
                    mutateForm { it.copy(username = action.value) }
                }

                is AddPasswordAction.PasswordChanged -> {
                    mutateForm { it.copy(password = action.value) }
                }

                is AddPasswordAction.PasswordVisibilityChanged -> {
                    mutateForm { it.copy(isPasswordVisible = action.visible) }
                }

                is AddPasswordAction.WebsiteUrlChanged -> {
                    mutateForm {
                        it.copy(websiteUrl = action.value)
                    }
                }

                is AddPasswordAction.NotesChanged -> {
                    mutateForm { it.copy(notes = action.value) }
                }

                is AddPasswordAction.TagsChanged -> {
                    mutateForm { it.copy(tags = action.value) }
                }

                AddPasswordAction.LoadAssociatedApps -> {
                    loadAssociatedApps()
                }

                is AddPasswordAction.AssociatedAppsSearchChanged -> {
                    val ready =
                        _uiState.value.availableApps as? AppPackagePickerUiState.Ready<AppPackagePickerItemUiModel> ?: return
                    _uiState.value =
                        _uiState.value.copy(
                            availableApps =
                                ready.search(
                                    action.query,
                                    AppPackagePickerItemUiModel::label,
                                    AppPackagePickerItemUiModel::packageName,
                                ),
                        )
                }

                is AddPasswordAction.AssociatedAppSelected -> {
                    mutateForm {
                        it.copy(associatedAppPackage = action.packageName)
                    }
                }

                AddPasswordAction.Save -> {
                    save()
                }
            }
        }

        private fun loadAssociatedApps() {
            if (!writeSession.canEdit()) return
            if (_uiState.value.availableApps is AppPackagePickerUiState.Ready ||
                _uiState.value.availableApps == AppPackagePickerUiState.Loading ||
                associatedAppsLoadJob?.isActive == true
            ) return
            val authentication = secureSessionAccessState.authenticationState.value
            _uiState.value = _uiState.value.copy(availableApps = AppPackagePickerUiState.Loading)
            associatedAppsLoadJob =
                viewModelScope.launch {
                    if (!secureSessionAccessState.hasFullSecureSessionAccess() ||
                        secureSessionAccessState.authenticationState.value !== authentication
                    ) {
                        _uiState.value = _uiState.value.copy(availableApps = AppPackagePickerUiState.NotRequested)
                        return@launch
                    }
                    try {
                        val apps =
                            installedAppDirectory.launchableApps().map { metadata ->
                                AppPackagePickerItemUiModel(metadata.label, metadata.packageName)
                            }
                        coroutineContext.ensureActive()
                        if (!secureSessionAccessState.hasFullSecureSessionAccess() ||
                            secureSessionAccessState.authenticationState.value !== authentication
                        ) {
                            return@launch
                        }
                        _uiState.value =
                            _uiState.value.copy(
                                availableApps =
                                    appPackagePickerReady(apps),
                            )
                    } catch (error: CancellationException) {
                        throw error
                    } catch (_: Exception) {
                        if (secureSessionAccessState.authenticationState.value === authentication) {
                            _uiState.value = _uiState.value.copy(availableApps = AppPackagePickerUiState.Failed)
                        }
                    }
                }
        }

        private fun mutateForm(transform: (AddPasswordFormState) -> AddPasswordFormState) {
            if (!writeSession.canEdit() || !canEditForm()) return
            val current = _uiState.value
            val form = transform(current.form)
            _uiState.value = current.copy(form = form)
        }

        private fun save() {
            val current = _uiState.value
            if (!canEditForm()) return
            if (!current.canSave) return
            writeSession.save { current.form.toEntryDraft() }
        }

        private fun clearSensitiveContent(reason: EditorWriteSessionClearReason) {
            associatedAppsLoadJob?.cancel()
            associatedAppsLoadJob = null
            _uiState.value =
                AddPasswordUiState(
                    loadPhase = reason.toEditorLoadPhase(editingExisting = editingEntryId != null),
                )
        }

        private fun canEditForm() = editingEntryId == null || _uiState.value.loadPhase == EditorLoadPhase.Ready

        private fun failEditLoad() {
            _uiState.value =
                _uiState.value.copy(
                    form = AddPasswordFormState(),
                    loadPhase = EditorLoadPhase.Failed,
                )
        }

        override fun onCleared() {
            writeSession.close()
        }
    }
