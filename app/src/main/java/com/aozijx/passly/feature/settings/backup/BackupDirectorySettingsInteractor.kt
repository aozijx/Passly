package com.aozijx.passly.feature.settings.backup

import com.aozijx.passly.core.platform.storage.PersistedDirectoryAccessStatus
import com.aozijx.passly.core.observability.LogStatements
import com.aozijx.passly.core.observability.Logger
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
    data class Granted(
        val treeUri: String,
    ) : BackupDirectoryGrantResult

    data object PermissionDenied : BackupDirectoryGrantResult

    data object Unavailable : BackupDirectoryGrantResult
}

enum class BackupDirectorySelectionResult {
    Saved,
    Cancelled,
    PermissionDenied,
    Unavailable,
}

class BackupDirectorySettingsInteractor
    @Inject
    constructor(
        private val settingsRepository: BackupDirectorySettingsRepository,
        private val directoryAccess: BackupDirectoryAccess,
        private val logger: Logger,
    ) {
        private val operationMutex = Mutex()

        suspend fun select(treeUri: String?): BackupDirectorySelectionResult =
            operationMutex.withLock {
                if (treeUri == null) return@withLock BackupDirectorySelectionResult.Cancelled
                val previousTreeUri = settingsRepository.backupDirectoryUri.first()
                when (val result = directoryAccess.acquire(treeUri)) {
                    is BackupDirectoryGrantResult.Granted -> {
                        persistGrantedDirectory(result.treeUri, previousTreeUri)
                    }

                    BackupDirectoryGrantResult.PermissionDenied -> {
                        BackupDirectorySelectionResult.PermissionDenied
                    }

                    BackupDirectoryGrantResult.Unavailable -> {
                        BackupDirectorySelectionResult.Unavailable
                    }
                }
            }

        private suspend fun persistGrantedDirectory(
            grantedTreeUri: String,
            previousTreeUri: String?,
        ): BackupDirectorySelectionResult {
            try {
                settingsRepository.setBackupDirectoryUri(grantedTreeUri)
            } catch (error: Throwable) {
                val persistedUri = readPersistedAfterFailure(error)
                if (persistedUri.isSuccess && persistedUri.getOrNull() != grantedTreeUri) {
                    releaseAfterFailure(grantedTreeUri, error)
                }
                if (error is CancellationException || error !is Exception) throw error
                if (persistedUri.getOrNull() != grantedTreeUri) {
                    return BackupDirectorySelectionResult.Unavailable
                }
            }

            previousTreeUri
                ?.takeIf { it.isNotBlank() && it != grantedTreeUri }
                ?.let { previous ->
                    // The new URI is already authoritative. Failure to release the old OS grant
                    // must not roll back by revoking the grant now referenced by preferences.
                    releaseAfterCommit(previous)
                }
            currentCoroutineContext().ensureActive()
            return BackupDirectorySelectionResult.Saved
        }

        suspend fun clear() =
            operationMutex.withLock {
                val previousTreeUri = settingsRepository.backupDirectoryUri.first()
                var clearFailure: Throwable? = null
                try {
                    settingsRepository.clearBackupDirectoryUri()
                } catch (failure: Throwable) {
                    clearFailure = failure
                }

                val persistedUri =
                    if (clearFailure == null) {
                        null
                    } else {
                        readPersistedAfterFailure(clearFailure)
                    }
                val shouldRelease =
                    clearFailure == null ||
                        (persistedUri?.isSuccess == true && persistedUri.getOrNull() != previousTreeUri)
                if (shouldRelease) {
                    previousTreeUri?.takeIf(String::isNotBlank)?.let { uri ->
                        if (clearFailure == null) {
                            releaseAfterCommit(uri)
                        } else {
                            releaseAfterFailure(uri, clearFailure)
                        }
                    }
                }
                clearFailure?.let { failure ->
                    if (failure is CancellationException || failure !is Exception) throw failure
                    if (persistedUri?.isSuccess != true || persistedUri.getOrNull() != null) throw failure
                }
                currentCoroutineContext().ensureActive()
            }

        private suspend fun readPersistedAfterFailure(primaryFailure: Throwable): Result<String?> =
            withContext(NonCancellable) {
                try {
                    Result.success(settingsRepository.backupDirectoryUri.first())
                } catch (failure: Throwable) {
                    when {
                        failure is CancellationException || failure !is Exception -> {
                            if (failure !== primaryFailure) failure.addSuppressed(primaryFailure)
                            throw failure
                        }

                        else -> {
                            if (failure !== primaryFailure) primaryFailure.addSuppressed(failure)
                            Result.failure(failure)
                        }
                    }
                }
            }

        private suspend fun releaseAfterFailure(
            treeUri: String,
            primaryFailure: Throwable,
        ) {
            withContext(NonCancellable) {
                try {
                    directoryAccess.release(treeUri)
                } catch (failure: Throwable) {
                    when {
                        failure is CancellationException || failure !is Exception -> {
                            if (failure !== primaryFailure) failure.addSuppressed(primaryFailure)
                            throw failure
                        }

                        else -> if (failure !== primaryFailure) primaryFailure.addSuppressed(failure)
                    }
                }
            }
        }

        private suspend fun releaseAfterCommit(treeUri: String) {
            withContext(NonCancellable) {
                try {
                    directoryAccess.release(treeUri)
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (failure: Exception) {
                    // The persisted URI is already authoritative; an ordinary cleanup failure
                    // must not report the settings write as uncommitted.
                    logger.log(LogStatements.Backup.directoryGrantReleaseFailed(failure))
                }
            }
        }
    }
