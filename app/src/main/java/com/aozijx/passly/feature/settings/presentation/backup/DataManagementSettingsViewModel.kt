package com.aozijx.passly.feature.settings.presentation.backup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aozijx.passly.domain.settings.port.BackupDirectorySettingsSource
import com.aozijx.passly.feature.settings.backup.BackupDirectorySelectionResult
import com.aozijx.passly.feature.settings.backup.BackupDirectorySettingsInteractor
import com.aozijx.passly.presentation.shared.event.PageEffects
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class DataManagementSettingsViewModel
    @Inject
    constructor(
        private val directorySource: BackupDirectorySettingsSource,
        private val directorySettings: BackupDirectorySettingsInteractor,
    ) : ViewModel() {
        private val pageEffects = PageEffects<DataManagementSettingsEffect>()

        private val _uiState =
            MutableStateFlow<DataManagementSettingsUiState>(
                DataManagementSettingsUiState.Loading,
            )
        val uiState: StateFlow<DataManagementSettingsUiState> = _uiState.asStateFlow()
        val effects = pageEffects.effects
        private var pendingDirectorySelection: PendingDirectorySelection? = null

        init {
            viewModelScope.launch {
                directorySource.backupDirectoryUri.collect { directoryUri ->
                    _uiState.value =
                        DataManagementSettingsUiState.Ready(
                            directoryUri = directoryUri,
                        )
                }
            }
        }

        fun onAction(action: DataManagementSettingsUiAction) {
            viewModelScope.launch {
                when (action) {
                    is DataManagementSettingsUiAction.BackupDirectoryPicked -> {
                        if (pendingDirectorySelection?.matches(action) == true &&
                            pageEffects.isActive(action.pageOwner)
                        ) {
                            pendingDirectorySelection = null
                            handleSelection(action.uri, action.pageOwner)
                        }
                    }

                    DataManagementSettingsUiAction.ClearBackupDirectory -> {
                        when (_uiState.value) {
                            DataManagementSettingsUiState.Loading -> Unit
                            is DataManagementSettingsUiState.Ready -> directorySettings.clear()
                        }
                    }
                }
            }
        }

        private suspend fun handleSelection(
            uri: String?,
            owner: Any,
        ) {
            when (directorySettings.select(uri)) {
                BackupDirectorySelectionResult.Saved -> {
                    Unit
                }

                BackupDirectorySelectionResult.Cancelled -> {
                    pageEffects.tryEmit(
                        owner,
                        DataManagementSettingsEffect.BackupDirectorySelectionCancelled,
                    )
                }

                BackupDirectorySelectionResult.PermissionDenied -> {
                    pageEffects.tryEmit(
                        owner,
                        DataManagementSettingsEffect.BackupDirectoryPermissionDenied,
                    )
                }

                BackupDirectorySelectionResult.Unavailable -> {
                    pageEffects.tryEmit(
                        owner,
                        DataManagementSettingsEffect.BackupDirectoryUnavailable,
                    )
                }
            }
        }

        fun onPageEntered(): Any {
            pendingDirectorySelection = null
            return pageEffects.enter()
        }

        fun beginDirectorySelection(owner: Any): String? {
            if (!pageEffects.isActive(owner) || pendingDirectorySelection != null) return null
            val requestId = UUID.randomUUID().toString()
            pendingDirectorySelection = PendingDirectorySelection(owner, requestId)
            return requestId
        }

        fun onPageExited(owner: Any) {
            pageEffects.exit(owner)
            if (pendingDirectorySelection?.pageOwner === owner) pendingDirectorySelection = null
        }

        private data class PendingDirectorySelection(
            val pageOwner: Any,
            val requestId: String,
        ) {
            fun matches(action: DataManagementSettingsUiAction.BackupDirectoryPicked): Boolean =
                pageOwner === action.pageOwner && requestId == action.requestId
        }
    }
