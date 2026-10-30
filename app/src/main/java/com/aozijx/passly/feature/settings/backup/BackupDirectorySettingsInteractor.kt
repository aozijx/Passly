package com.aozijx.passly.feature.settings.backup

import com.aozijx.passly.core.platform.storage.PersistedDirectoryAccessStatus
import com.aozijx.passly.domain.settings.port.BackupDirectorySettingsRepository
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

fun interface BackupDirectoryStatusReader {
    fun inspect(treeUri: String?): PersistedDirectoryAccessStatus
}

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
    private val operationMutex = Mutex()

    suspend fun select(treeUri: String?): BackupDirectorySelectionResult = operationMutex.withLock {
        if (treeUri == null) return@withLock BackupDirectorySelectionResult.Cancelled
        val previousTreeUri = settingsRepository.backupDirectoryUri.first()
        when (val result = directoryAccess.acquire(treeUri)) {
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
    ): BackupDirectorySelectionResult {
        try {
            settingsRepository.setBackupDirectoryUri(grantedTreeUri)
        } catch (error: Throwable) {
            val persistedUri = withContext(NonCancellable) {
                runCatching { settingsRepository.backupDirectoryUri.first() }
            }
            if (persistedUri.isSuccess && persistedUri.getOrNull() != grantedTreeUri) {
                withContext(NonCancellable) {
                    runCatching { directoryAccess.release(grantedTreeUri) }
                }
            }
            if (error is CancellationException) throw error
            if (persistedUri.getOrNull() != grantedTreeUri) {
                return BackupDirectorySelectionResult.Unavailable
            }
        }

        previousTreeUri
            ?.takeIf { it.isNotBlank() && it != grantedTreeUri }
            ?.let { previous ->
                // The new URI is already authoritative. Failure to release the old OS grant
                // must not roll back by revoking the grant now referenced by preferences.
                withContext(NonCancellable) {
                    runCatching { directoryAccess.release(previous) }
                }
            }
        currentCoroutineContext().ensureActive()
        return BackupDirectorySelectionResult.Saved
    }

    suspend fun clear() = operationMutex.withLock {
        val previousTreeUri = settingsRepository.backupDirectoryUri.first()
        var clearFailure: Throwable? = null
        try {
            settingsRepository.clearBackupDirectoryUri()
        } catch (failure: Throwable) {
            clearFailure = failure
        }

        val persistedUri = if (clearFailure == null) null else {
            val persistedUri = withContext(NonCancellable) {
                runCatching { settingsRepository.backupDirectoryUri.first() }
            }
            persistedUri
        }
        val shouldRelease = clearFailure == null ||
            (persistedUri?.isSuccess == true && persistedUri.getOrNull() != previousTreeUri)
        if (shouldRelease) previousTreeUri?.takeIf(String::isNotBlank)?.let { uri ->
            withContext(NonCancellable) {
                runCatching { directoryAccess.release(uri) }
            }
        }
        clearFailure?.let { throw it }
        currentCoroutineContext().ensureActive()
    }
}
