package com.aozijx.passly.feature.settings.backup

import com.aozijx.passly.domain.settings.port.BackupDirectorySettingsRepository
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

interface BackupDirectoryAccess {
    suspend fun acquire(treeUri: String): BackupDirectoryGrantResult
    suspend fun release(treeUri: String)
}

sealed interface BackupDirectoryGrantResult {
    data class Granted(val treeUri: String) : BackupDirectoryGrantResult
    data object PermissionDenied : BackupDirectoryGrantResult
    data object Unavailable : BackupDirectoryGrantResult
}

enum class BackupDirectorySelectionResult {
    Saved,
    Cancelled,
    PermissionDenied,
    Unavailable,
}

class BackupDirectorySettingsInteractor @Inject constructor(
    private val settingsRepository: BackupDirectorySettingsRepository,
    private val directoryAccess: BackupDirectoryAccess,
) {
    val directoryUri: Flow<String?> = settingsRepository.backupDirectoryUri

    suspend fun select(treeUri: String?): BackupDirectorySelectionResult {
        if (treeUri == null) return BackupDirectorySelectionResult.Cancelled
        val previousTreeUri = directoryUri.first()
        return when (val result = directoryAccess.acquire(treeUri)) {
            is BackupDirectoryGrantResult.Granted -> {
                persistGrantedDirectory(result.treeUri, previousTreeUri)
            }
            BackupDirectoryGrantResult.PermissionDenied ->
                BackupDirectorySelectionResult.PermissionDenied
            BackupDirectoryGrantResult.Unavailable ->
                BackupDirectorySelectionResult.Unavailable
        }
    }

    private suspend fun persistGrantedDirectory(
        grantedTreeUri: String,
        previousTreeUri: String?,
    ): BackupDirectorySelectionResult = try {
        settingsRepository.setBackupDirectoryUri(grantedTreeUri)
        previousTreeUri
            ?.takeIf { it.isNotBlank() && it != grantedTreeUri }
            ?.let { directoryAccess.release(it) }
        BackupDirectorySelectionResult.Saved
    } catch (error: Throwable) {
        if (error is CancellationException) throw error
        directoryAccess.release(grantedTreeUri)
        BackupDirectorySelectionResult.Unavailable
    }

    suspend fun clear(treeUri: String?) {
        treeUri?.takeIf(String::isNotBlank)?.let { directoryAccess.release(it) }
        settingsRepository.clearBackupDirectoryUri()
    }
}
