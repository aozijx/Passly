package com.aozijx.passly.feature.backup.internal.presentation

import android.net.Uri
import com.aozijx.passly.feature.backup.internal.archive.platform.BackupStorageSupport
import com.aozijx.passly.core.error.mapping.fromThrowable
import com.aozijx.passly.core.error.model.AppError
import com.aozijx.passly.core.error.model.BackupFailed
import com.aozijx.passly.core.error.result.AppResult
import com.aozijx.passly.domain.access.port.AuthenticationManager
import com.aozijx.passly.domain.access.model.AuthenticationPurpose
import com.aozijx.passly.domain.access.model.AuthenticationRequest
import com.aozijx.passly.domain.access.model.AuthenticationResult
import com.aozijx.passly.feature.backup.internal.model.BackupExportOptions
import com.aozijx.passly.feature.backup.internal.model.BackupExportRequest
import com.aozijx.passly.feature.backup.internal.model.BackupExportUiFormat
import com.aozijx.passly.feature.backup.internal.model.BackupImportRequest
import com.aozijx.passly.feature.backup.internal.archive.BackupArchiveService
import com.aozijx.passly.domain.settings.port.AppSettingsRepository
import com.aozijx.passly.feature.backup.presentation.contract.BackupUiState
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/** Executes Backup side effects while the ViewModel remains a state-machine boundary. */
/** Application use case boundary for backup flows; the ViewModel only dispatches state. */
internal class BackupOperationUseCase @Inject constructor(
    private val settingsRepository: AppSettingsRepository,
    private val backupService: BackupArchiveService,
    private val storageSupport: BackupStorageSupport,
    private val authenticationManager: AuthenticationManager,
) {
    fun buildExportFileName(format: BackupExportUiFormat): String =
        storageSupport.buildBackupFileName(format.extension)

    suspend fun checkDirectoryWritable(uri: String?): BackupExecutionResult {
        if (uri.isNullOrBlank()) return BackupExecutionResult.Failure(BackupFailed())
        return when (val result = backupService.checkDirectoryWritable(uri)) {
            is AppResult.Success -> BackupExecutionResult.Success
            is AppResult.Failure -> BackupExecutionResult.Failure(result.error)
        }
    }

    suspend fun exportToConfiguredDirectory(state: BackupUiState): BackupExecutionResult {
        authenticate(AuthenticationPurpose.BACKUP_EXPORT).let { authResult ->
            if (authResult != BackupExecutionResult.Success) return authResult
        }
        val directoryUri = settingsRepository.settings.first().backup.directoryTreeUri
            ?: return BackupExecutionResult.Failure(BackupFailed())
        if (directoryUri.isBlank()) return BackupExecutionResult.Failure(BackupFailed())

        val fileName = state.pendingExportFileName
            ?: buildExportFileName(state.selectedExportFormat)
        val target = storageSupport.createNamedExportTarget(
            directoryTreeUri = directoryUri,
            fileName = fileName,
            mimeType = state.selectedExportFormat.mimeType,
        ).getOrElse { error ->
            return BackupExecutionResult.Failure(AppError.fromThrowable(error))
        }
        return performOperation(
            state = state.copy(
                backupUri = target.fileUri,
                pendingExportFileName = target.fileName,
                deleteTargetOnFailure = true,
            ),
            targetUri = target.fileUri,
        )
    }

    suspend fun executePending(state: BackupUiState): BackupExecutionResult {
        val targetUri = state.backupUri ?: return BackupExecutionResult.Failure(BackupFailed())
        val purpose = if (state.isExporting) {
            AuthenticationPurpose.BACKUP_EXPORT
        } else {
            AuthenticationPurpose.BACKUP_IMPORT
        }
        authenticate(purpose).let { authResult ->
            if (authResult != BackupExecutionResult.Success) return authResult
        }
        return performOperation(state, targetUri)
    }

    private suspend fun authenticate(purpose: AuthenticationPurpose): BackupExecutionResult =
        when (
            authenticationManager.authenticate(
                AuthenticationRequest(purpose = purpose),
            )
        ) {
            is AuthenticationResult.Success -> BackupExecutionResult.Success
            is AuthenticationResult.Cancelled -> BackupExecutionResult.Cancelled
            is AuthenticationResult.Failure -> BackupExecutionResult.Failure(BackupFailed())
        }

    private suspend fun performOperation(
        state: BackupUiState,
        targetUri: Uri,
    ): BackupExecutionResult {
        val password = state.backupPassword.takeUnless { it.isEmpty }?.toCharArray()
        return try {
            val result = if (state.isExporting) {
                backupService.export(
                    BackupExportRequest(
                        targetUri = targetUri.toString(),
                        format = state.selectedExportFormat.formatId,
                        password = password,
                        options = BackupExportOptions(
                            includeIcons =
                                state.includeIcons && state.selectedExportFormat.supportsResources,
                            includeAttachments =
                                state.includeAttachments &&
                                        state.selectedExportFormat.supportsResources,
                            includeDeleted = state.includeDeleted,
                            includedEntryTypes = state.includedEntryTypes,
                        ),
                    ),
                )
            } else {
                backupService.import(
                    BackupImportRequest(
                        sourceUri = targetUri.toString(),
                        mode = state.importMode,
                        format = null,
                        password = password,
                    ),
                )
            }
            when (result) {
                is AppResult.Success -> BackupExecutionResult.Success
                is AppResult.Failure -> {
                    deleteFailedExport(state, targetUri)
                    BackupExecutionResult.Failure(result.error)
                }
            }
        } catch (error: Exception) {
            deleteFailedExport(state, targetUri)
            BackupExecutionResult.Failure(AppError.fromThrowable(error))
        } finally {
            password?.fill('\u0000')
        }
    }

    private fun deleteFailedExport(state: BackupUiState, targetUri: Uri) {
        if (state.isExporting && state.deleteTargetOnFailure) {
            storageSupport.deleteDocument(targetUri)
        }
    }

}

internal sealed interface BackupExecutionResult {
    data object Success : BackupExecutionResult
    data object Cancelled : BackupExecutionResult
    data class Failure(val error: AppError) : BackupExecutionResult
}

internal enum class BackupOperation {
    EXPORT,
    IMPORT,
    DIRECTORY_CHECK,
}
