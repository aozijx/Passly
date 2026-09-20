package com.aozijx.passly.feature.vault.detail

import com.aozijx.passly.core.error.model.Conflict
import com.aozijx.passly.core.error.model.NotFound
import com.aozijx.passly.core.error.model.ValidationError
import com.aozijx.passly.core.error.result.AppResult
import com.aozijx.passly.domain.entry.model.Entry
import com.aozijx.passly.domain.entry.model.EntryIcon
import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.model.EntryUpdate
import com.aozijx.passly.domain.entry.model.FieldKey
import com.aozijx.passly.domain.entry.model.credential.CardCredential
import com.aozijx.passly.domain.entry.model.credential.LoginCredential
import com.aozijx.passly.domain.entry.port.EntryCommandRepository
import com.aozijx.passly.domain.entry.port.EntryQueryRepository
import javax.inject.Inject
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal sealed interface DetailEntryEdit {
    data class SetTitle(val value: String) : DetailEntryEdit
    data object ToggleFavorite : DetailEntryEdit
    data class SetSensitiveField(
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
) {
    private val editMutex = Mutex()

    suspend fun edit(
        entryId: EntryId,
        edit: DetailEntryEdit,
    ): AppResult<Entry> = editMutex.withLock {
        var conflictRetries = 0
        while (true) {
            val latest = entryQueryRepository.getById(entryId)
                ?: return@withLock AppResult.Failure(NotFound())
            val updated = edit.applyTo(latest)
                ?: return@withLock AppResult.Failure(ValidationError())
            when (
                val result = entryCommandRepository.updateEntry(
                    id = entryId,
                    expectedVersion = latest.version,
                    changes = EntryUpdate(
                        profile = updated.profile,
                        secret = updated.secret,
                    ),
                )
            ) {
                is AppResult.Success -> {
                    val persisted = entryQueryRepository.getById(entryId)
                        ?: return@withLock AppResult.Failure(NotFound())
                    return@withLock AppResult.Success(persisted)
                }

                is AppResult.Failure -> {
                    if (result.error is Conflict && conflictRetries == 0) {
                        conflictRetries += 1
                        continue
                    }
                    return@withLock result
                }
            }
        }
        error("Unreachable detail entry edit state")
    }
}

internal fun DetailEntryEdit.applyTo(entry: Entry): Entry? = when (this) {
    is DetailEntryEdit.SetTitle -> entry.copy(
        profile = entry.profile.copy(title = value),
    )
    DetailEntryEdit.ToggleFavorite -> entry.copy(
        profile = entry.profile.copy(favorite = !entry.favorite),
    )
    is DetailEntryEdit.SetSensitiveField -> applySensitiveField(entry)
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

private fun DetailEntryEdit.SetSensitiveField.applySensitiveField(entry: Entry): Entry? =
    when (fieldKey) {
        FieldKey.USERNAME,
        FieldKey.CARD_HOLDER,
        -> entry.copy(profile = entry.profile.copy(username = value))

        FieldKey.PASSWORD -> when (entry.type) {
            EntryType.WIFI -> entry.secret.wifi?.let { wifi ->
                entry.copy(
                    secret = entry.secret.copy(
                        credential = wifi.copy(password = value),
                    ),
                )
            }
            else -> entry.copy(
                secret = entry.secret.copy(
                    credential = entry.secret.login?.copy(password = value)
                        ?: LoginCredential(password = value),
                ),
            )
        }

        FieldKey.CARD_NUMBER -> entry.copy(
            secret = entry.secret.copy(
                credential = (entry.secret.card ?: CardCredential()).copy(cardNumber = value),
            ),
        )
        FieldKey.CARD_CVV -> entry.copy(
            secret = entry.secret.copy(
                credential = (entry.secret.card ?: CardCredential()).copy(cardCvv = value),
            ),
        )
        FieldKey.SSH_PASSPHRASE -> entry.secret.ssh?.let { ssh ->
            entry.copy(
                secret = entry.secret.copy(
                    credential = ssh.copy(passphrase = value),
                ),
            )
        }
        else -> null
    }
