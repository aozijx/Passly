package com.aozijx.passly.feature.settings.presentation.backup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aozijx.passly.app.settings.RecoverableRead
import com.aozijx.passly.app.settings.RecoverableReadState
import com.aozijx.passly.core.observability.LogStatements
import com.aozijx.passly.core.observability.Logger
import com.aozijx.passly.domain.settings.port.BackupDirectorySettingsSource
import com.aozijx.passly.feature.settings.backup.BackupDirectorySelectionResult
import com.aozijx.passly.feature.settings.backup.BackupDirectorySettingsInteractor
import com.aozijx.passly.presentation.shared.event.PageEffects
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@HiltViewModel
class DataManagementSettingsViewModel
    @Inject
    constructor(
        private val directorySource: BackupDirectorySettingsSource,
        private val directorySettings: BackupDirectorySettingsInteractor,
        private val logger: Logger,
    ) : ViewModel() {
        private val pageEffects = PageEffects<DataManagementSettingsEffect>()

        val effects = pageEffects.effects
        private var pendingDirectorySelection: PendingDirectorySelection? = null
        private var clearInFlight = false
        private val directoryRead =
            RecoverableRead(
                source =
                    directorySource.backupDirectoryUri.map { uri ->
                        uri?.takeIf(String::isNotBlank)
                            ?.let(BackupDirectoryUiState::Configured)
                            ?: BackupDirectoryUiState.Unconfigured
                    },
                scope = viewModelScope,
            )

        val uiState: StateFlow<RecoverableReadState<BackupDirectoryUiState>> = directoryRead.state

        fun onAction(action: DataManagementSettingsUiAction) {
            val clearOwner =
                if (action == DataManagementSettingsUiAction.ClearBackupDirectory) {
                    pageEffects.currentOwner
                } else {
                    null
                }
            viewModelScope.launch {
                when (action) {
                    DataManagementSettingsUiAction.RetryRead -> {
                        directoryRead.retry()
                    }

                    is DataManagementSettingsUiAction.BackupDirectoryPicked -> {
                        if (pendingDirectorySelection?.matches(action) == true &&
                            pageEffects.isActive(action.pageOwner)
                        ) {
                            pendingDirectorySelection = null
                            handleSelection(action.uri, action.pageOwner)
                        }
                    }

                    DataManagementSettingsUiAction.ClearBackupDirectory -> {
                        if (clearOwner != null && pageEffects.isActive(clearOwner) && !clearInFlight &&
                            uiState.value is RecoverableReadState.Ready
                        ) {
                            clearInFlight = true
                            try {
                                directorySettings.clear()
                                pageEffects.tryEmit(clearOwner, DataManagementSettingsEffect.BackupDirectoryCleared)
                            } catch (cancelled: CancellationException) {
                                throw cancelled
                            } catch (failure: Exception) {
                                logger.log(LogStatements.Backup.directorySettingsOperationFailed(failure))
                                pageEffects.tryEmit(
                                    clearOwner,
                                    DataManagementSettingsEffect.BackupDirectoryOperationFailed,
                                )
                            } finally {
                                clearInFlight = false
                            }
                        }
                    }
                }
            }
        }

        private suspend fun handleSelection(
            uri: String?,
            owner: Any,
        ) {
            try {
                when (directorySettings.select(uri)) {
                    BackupDirectorySelectionResult.Saved -> Unit

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
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                logger.log(LogStatements.Backup.directorySettingsOperationFailed(failure))
                pageEffects.tryEmit(owner, DataManagementSettingsEffect.BackupDirectoryOperationFailed)
            }
        }

        fun onPageEntered(): Any {
            pendingDirectorySelection = null
            return pageEffects.enter()
        }

        fun beginDirectorySelection(owner: Any): String? {
            if (!pageEffects.isActive(owner) || pendingDirectorySelection != null ||
                uiState.value !is RecoverableReadState.Ready
            ) {
                return null
            }
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
