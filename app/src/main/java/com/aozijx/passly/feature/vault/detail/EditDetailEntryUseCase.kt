package com.aozijx.passly.feature.vault.detail

import com.aozijx.passly.core.error.model.Conflict
import com.aozijx.passly.core.error.model.NotFound
import com.aozijx.passly.core.error.model.SessionModeRestricted
import com.aozijx.passly.core.error.model.ValidationError
import com.aozijx.passly.core.error.result.AppResult
import com.aozijx.passly.core.observability.Logger
import com.aozijx.passly.core.observability.LogStatements
import com.aozijx.passly.core.observability.Operations
import com.aozijx.passly.core.observability.monotonicTimeMs
import com.aozijx.passly.domain.access.port.SecureSessionAccessState
import com.aozijx.passly.domain.entry.model.Entry
import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.model.EntryUpdate
import com.aozijx.passly.domain.entry.policy.EntryEdit
import com.aozijx.passly.domain.entry.policy.EntryEditResult
import com.aozijx.passly.domain.entry.port.EntryCommandRepository
import com.aozijx.passly.domain.entry.port.EntryQueryRepository
import javax.inject.Inject
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.CancellationException

internal class EditDetailEntryUseCase @Inject constructor(
    private val entryQueryRepository: EntryQueryRepository,
    private val entryCommandRepository: EntryCommandRepository,
    private val secureSessionAccessState: SecureSessionAccessState,
    private val logger: Logger,
) {
    private val editMutex = Mutex()

    suspend fun edit(
        entryId: EntryId,
        edit: EntryEdit,
    ): AppResult<Entry> = logger.inSpan(Operations.VaultEntryUpdate) {
        val startedAt = monotonicTimeMs()
        var entryType: EntryType? = null
        logger.log(LogStatements.Vault.entryUpdateStarted())
        val result = try {
            editMutex.withLock { performEdit(entryId, edit) { entryType = it } }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Exception) {
            logger.log(
                LogStatements.Vault.entryUpdateFailed(failure, entryType, monotonicTimeMs() - startedAt),
            )
            throw failure
        }
        when (result) {
            is AppResult.Success -> logger.log(
                LogStatements.Vault.entryUpdateCompleted(entryType, monotonicTimeMs() - startedAt),
            )
            is AppResult.Failure -> logger.log(
                LogStatements.Vault.entryUpdateFailed(result.error, entryType, monotonicTimeMs() - startedAt),
            )
        }
        result
    }

    private suspend fun performEdit(
        entryId: EntryId,
        edit: EntryEdit,
        observeType: (EntryType) -> Unit,
    ): AppResult<Entry> {
        if (!secureSessionAccessState.hasFullSecureSessionAccess()) {
            return AppResult.Failure(SessionModeRestricted())
        }
        var conflictRetries = 0
        while (true) {
            val latest = entryQueryRepository.getById(entryId)
                ?: return AppResult.Failure(NotFound())
            observeType(latest.type)
            val updated = when (val applied = edit.applyTo(latest)) {
                is EntryEditResult.Updated -> applied.entry
                is EntryEditResult.Invalid, EntryEditResult.Unsupported -> return AppResult.Failure(ValidationError())
            }
            val changes = updated.changesFrom(latest)
                ?: return AppResult.Success(latest)
            when (
                val result = entryCommandRepository.updateEntry(
                    id = entryId,
                    expectedVersion = latest.version,
                    changes = changes,
                )
            ) {
                is AppResult.Success -> {
                    val persisted = entryQueryRepository.getById(entryId)
                        ?: return AppResult.Failure(NotFound())
                    return AppResult.Success(persisted)
                }

                is AppResult.Failure -> {
                    if (result.error is Conflict && conflictRetries == 0) {
                        conflictRetries += 1
                        continue
                    }
                    return result
                }
            }
        }
        error("Unreachable detail entry edit state")
    }
}

private fun Entry.changesFrom(previous: Entry): EntryUpdate? {
    val changedProfile = profile.takeIf { it != previous.profile }
    val changedSecret = secret.takeIf { it != previous.secret }
    if (changedProfile == null && changedSecret == null) return null
    return EntryUpdate(
        profile = changedProfile,
        secret = changedSecret,
    )
}
