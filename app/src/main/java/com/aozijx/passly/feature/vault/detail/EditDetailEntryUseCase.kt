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
import com.aozijx.passly.domain.entry.model.EntryIcon
import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.model.EntryUpdate
import com.aozijx.passly.domain.entry.model.FieldKey
import com.aozijx.passly.domain.entry.policy.EntryFieldEditor
import com.aozijx.passly.domain.entry.policy.EntryFieldEditResult
import com.aozijx.passly.domain.entry.port.EntryCommandRepository
import com.aozijx.passly.domain.entry.port.EntryQueryRepository
import javax.inject.Inject
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.CancellationException

internal sealed interface DetailEntryEdit {
    data class SetTitle(val value: String) : DetailEntryEdit
    data object ToggleFavorite : DetailEntryEdit
    data class SetFieldValue(
        val fieldKey: FieldKey,
        val value: String,
    ) : DetailEntryEdit
    data class SetNotes(val value: String?) : DetailEntryEdit
    data class SetPrimaryUrl(val value: String?) : DetailEntryEdit
    data class SetApplicationIds(val values: Set<String>) : DetailEntryEdit
    data class SetTags(val values: Set<String>) : DetailEntryEdit
    data class SetIcon(val value: EntryIcon) : DetailEntryEdit
}

internal class EditDetailEntryUseCase @Inject constructor(
    private val entryQueryRepository: EntryQueryRepository,
    private val entryCommandRepository: EntryCommandRepository,
    private val secureSessionAccessState: SecureSessionAccessState,
    private val logger: Logger,
) {
    private val editMutex = Mutex()

    suspend fun edit(
        entryId: EntryId,
        edit: DetailEntryEdit,
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
        edit: DetailEntryEdit,
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
            val updated = edit.applyTo(latest)
                ?: return AppResult.Failure(ValidationError())
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

internal fun DetailEntryEdit.applyTo(entry: Entry): Entry? = when (this) {
    is DetailEntryEdit.SetTitle -> if (entry.type == EntryType.WIFI) {
        entry.secret.wifi?.let { wifi ->
            entry.copy(
                profile = entry.profile.copy(title = value),
                secret = entry.secret.copy(credential = wifi.copy(ssid = value)),
            )
        }
    } else {
        entry.copy(profile = entry.profile.copy(title = value))
    }
    DetailEntryEdit.ToggleFavorite -> entry.copy(
        profile = entry.profile.copy(favorite = !entry.favorite),
    )
    is DetailEntryEdit.SetFieldValue -> applyFieldValue(entry)
    is DetailEntryEdit.SetNotes -> entry.copy(
        secret = entry.secret.copy(notes = value),
    )
    is DetailEntryEdit.SetPrimaryUrl -> entry.copy(
        profile = entry.profile.copy(
            associations = entry.associations.copy(primaryUrl = value),
        ),
    )
    is DetailEntryEdit.SetApplicationIds -> entry.copy(
        profile = entry.profile.copy(
            associations = entry.associations.copy(applicationIds = values),
        ),
    )
    is DetailEntryEdit.SetTags -> entry.copy(
        profile = entry.profile.copy(tags = values),
    )
    is DetailEntryEdit.SetIcon -> entry.copy(
        profile = entry.profile.copy(icon = value),
    )
}

private fun DetailEntryEdit.SetFieldValue.applyFieldValue(entry: Entry): Entry? =
    when (val result = EntryFieldEditor.edit(entry, fieldKey, value)) {
        is EntryFieldEditResult.Updated -> result.entry
        EntryFieldEditResult.Unsupported, is EntryFieldEditResult.Invalid -> null
    }
