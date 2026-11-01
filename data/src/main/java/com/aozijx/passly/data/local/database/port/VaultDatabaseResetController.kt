package com.aozijx.passly.data.local.database.port

import android.content.Context
import com.aozijx.passly.core.platform.VaultResourcePaths
import com.aozijx.passly.core.observability.LogStatements
import com.aozijx.passly.core.observability.Logger
import com.aozijx.passly.core.observability.Operations
import com.aozijx.passly.data.local.database.DatabaseSchema
import com.aozijx.passly.runtime.session.DatabaseSessionLifecycle
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class VaultDatabaseResetController @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val databaseSession: DatabaseSessionLifecycle,
    private val logger: Logger,
) : DatabaseResetController {
    override suspend fun reset(): Throwable? {
        currentCoroutineContext().ensureActive()
        val outcome: Result<Throwable?> = withContext(Dispatchers.IO) {
            withContext(NonCancellable) {
                try {
                    logger.inSpan(Operations.DatabaseReset) {
                        executeReset()
                    }
                } catch (cancelled: CancellationException) {
                    Result.failure(cancelled)
                } catch (failure: Error) {
                    Result.failure(failure)
                }
            }
        }
        return outcome.fold(
            onSuccess = { error ->
                currentCoroutineContext().ensureActive()
                error
            },
            onFailure = { failure ->
                when (failure) {
                    is CancellationException -> throw failure
                    is Error -> {
                        withContext(NonCancellable) { sealForFatalFailure(failure) }
                        throw failure
                    }
                    else -> throw failure
                }
            },
        )
    }

    private suspend fun executeReset(): Result<Throwable?> {
        var error: Throwable? = null
        var cancellation: CancellationException? = null
        var fatal: Error? = null
        var reopen = false

        try {
            when (val sealFailure = databaseSession.seal().exceptionOrNull()) {
                null -> {
                    reopen = true
                    try {
                        deleteDatabaseFiles()
                        VaultResourcePaths.RESOURCE_DIRECTORY_NAMES.forEach(::deleteVaultFileDirectory)
                    } catch (cancelled: CancellationException) {
                        cancellation = cancelled
                    } catch (failure: Error) {
                        fatal = failure
                        reopen = false
                    } catch (failure: Throwable) {
                        error = failure
                    }
                }
                is CancellationException -> {
                    cancellation = sealFailure
                    reopen = true
                }
                is Error -> fatal = sealFailure
                else -> error = sealFailure
            }
        } catch (cancelled: CancellationException) {
            cancellation = cancelled
            reopen = true
        } catch (failure: Error) {
            fatal = failure
        } catch (failure: Exception) {
            error = failure
            reopen = true
        }

        if (reopen && fatal == null) {
            try {
                when (val reopenFailure = databaseSession.unlock()) {
                    null -> Unit
                    is CancellationException -> cancellation = cancellation ?: reopenFailure
                    is Error -> fatal = reopenFailure
                    else -> error?.addSuppressedUnlessSame(reopenFailure) ?: run { error = reopenFailure }
                }
            } catch (cancelled: CancellationException) {
                cancellation = cancellation ?: cancelled
            } catch (failure: Error) {
                fatal = failure
            } catch (failure: Exception) {
                error?.addSuppressedUnlessSame(failure) ?: run { error = failure }
            }
        }

        when (val failure = error) {
            is Error -> {
                fatal = fatal ?: failure
                error = null
            }
            null -> Unit
            else -> try {
                logger.log(LogStatements.Database.resetFailed(failure))
            } catch (logFailure: Error) {
                fatal = fatal ?: logFailure
            } catch (_: CancellationException) {
                // Logging cannot interrupt the reset cleanup already in progress.
            } catch (_: Exception) {
                // A failed diagnostic must not change an ordinary reset result.
            }
        }

        fatal?.let { fatalError ->
            error?.let { fatalError.addSuppressedUnlessSame(it) }
            return Result.failure(fatalError)
        }
        cancellation?.let { return Result.failure(it) }
        return Result.success(error)
    }

    private suspend fun sealForFatalFailure(fatal: Error) {
        if (databaseSession.lockState == com.aozijx.passly.runtime.session.SecureSessionState.SEALED) return
        try {
            databaseSession.seal().exceptionOrNull()?.let { fatal.addSuppressedUnlessSame(it) }
        } catch (cleanupFailure: Throwable) {
            fatal.addSuppressedUnlessSame(cleanupFailure)
        }
    }

    private fun Throwable.addSuppressedUnlessSame(other: Throwable) {
        if (this !== other) addSuppressed(other)
    }

    private fun deleteDatabaseFiles() {
        val databaseFile = context.getDatabasePath(DatabaseSchema.DATABASE_NAME)
        val candidates = listOf(
            databaseFile,
            File(databaseFile.path + "-wal"),
            File(databaseFile.path + "-shm"),
            File(databaseFile.path + "-journal"),
        )
        context.deleteDatabase(DatabaseSchema.DATABASE_NAME)
        val remaining = candidates.filter(File::exists)
        if (remaining.isNotEmpty()) {
            throw IOException("Unable to delete database files: ${remaining.joinToString { it.name }}")
        }
    }

    private fun deleteVaultFileDirectory(name: String) {
        val filesRoot = context.filesDir.canonicalFile
        val target = File(filesRoot, name).canonicalFile
        require(target.parentFile == filesRoot) { "Vault cleanup target escaped files directory" }
        if (target.exists() && !target.deleteRecursively()) {
            throw IOException("Unable to delete Vault file directory: $name")
        }
    }
}
