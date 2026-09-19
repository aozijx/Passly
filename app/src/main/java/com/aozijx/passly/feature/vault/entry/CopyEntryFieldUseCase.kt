package com.aozijx.passly.feature.vault.entry

import javax.inject.Inject

import com.aozijx.passly.domain.access.model.AuthenticationPurpose
import com.aozijx.passly.domain.access.model.AuthorizationPermit
import com.aozijx.passly.domain.access.model.AuthorizationResult
import com.aozijx.passly.domain.access.model.AuthorizationScope
import com.aozijx.passly.domain.access.model.SensitiveAccessAction
import com.aozijx.passly.domain.access.port.AuthorizationGate
import com.aozijx.passly.domain.clipboard.port.SensitiveClipboardWriter
import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.entry.model.FieldKey
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.model.sensitive.SensitiveFieldKey
import com.aozijx.passly.domain.entry.model.activity.ActivityType
import com.aozijx.passly.domain.entry.policy.EntryFieldReader
import com.aozijx.passly.domain.entry.policy.EntryTypeDefinitions
import com.aozijx.passly.domain.entry.port.EntryQueryRepository
import com.aozijx.passly.domain.entry.port.ActivityRecorder
import com.aozijx.passly.domain.entry.port.SensitiveFieldRepository

internal class CopyEntryFieldUseCase @Inject constructor(
    private val authorizationGate: AuthorizationGate,
    private val entryQueryRepository: EntryQueryRepository,
    private val entryFieldReader: EntryFieldReader,
    private val sensitiveFieldRepository: SensitiveFieldRepository,
    private val clipboardWriter: SensitiveClipboardWriter,
    private val activityRecorder: ActivityRecorder,
) {
    suspend operator fun invoke(
        entryId: EntryId,
        entryType: EntryType,
        fieldKey: FieldKey,
    ): CopyEntryFieldResult {
        val sensitiveKey = EntryTypeDefinitions.sensitiveStorageKey(entryType, fieldKey)
        val scope = sensitiveKey?.let {
            AuthorizationScope.SensitiveFields(
                entryId = entryId,
                fieldKeys = setOf(it),
                action = SensitiveAccessAction.COPY,
            )
        } ?: AuthorizationScope.Global(AuthenticationPurpose.COPY_SECRET)

        val result = when (val authorization = authorizationGate.authorize(scope) { permit ->
            if (sensitiveKey == null) {
                val entry = entryQueryRepository.getById(entryId)
                    ?: return@authorize CopyEntryFieldResult.Unavailable
                val value = entryFieldReader.getFieldValue(entry, fieldKey)
                    ?.takeIf(String::isNotBlank)
                    ?: return@authorize CopyEntryFieldResult.Unavailable
                clipboardWriter.writeSensitive(value)
                CopyEntryFieldResult.Copied
            } else {
                copySensitiveField(entryId, sensitiveKey, permit)
            }
        }) {
            is AuthorizationResult.Allowed -> authorization.value
            is AuthorizationResult.Denied,
            AuthorizationResult.Cancelled,
            -> CopyEntryFieldResult.NotAuthorized
        }
        if (result == CopyEntryFieldResult.Copied) {
            activityRecorder.recordUsage(entryId.value, fieldKey.copyActivityType())
        }
        return result
    }

    private suspend fun copySensitiveField(
        entryId: EntryId,
        key: SensitiveFieldKey,
        permit: AuthorizationPermit,
    ): CopyEntryFieldResult {
        val revealed = sensitiveFieldRepository.reveal(
            entryId = entryId,
            key = key,
            action = SensitiveAccessAction.COPY,
            permit = permit,
        ) ?: return CopyEntryFieldResult.Unavailable
        return try {
            val chars = revealed.value.toCharArray()
            try {
                if (chars.isEmpty()) {
                    CopyEntryFieldResult.Unavailable
                } else {
                    clipboardWriter.writeSensitive(String(chars))
                    CopyEntryFieldResult.Copied
                }
            } finally {
                chars.fill('\u0000')
            }
        } finally {
            revealed.value.wipe()
        }
    }

    private fun FieldKey.copyActivityType(): ActivityType = when (this) {
        FieldKey.USERNAME, FieldKey.CARD_HOLDER, FieldKey.WIFI_SSID -> ActivityType.COPY_USERNAME
        else -> ActivityType.COPY_PASSWORD
    }
}

internal class CopyOtpCodeUseCase @Inject constructor(
    private val authorizationGate: AuthorizationGate,
    private val clipboardWriter: SensitiveClipboardWriter,
    private val activityRecorder: ActivityRecorder,
) {
    suspend operator fun invoke(
        entryId: EntryId,
        codeProvider: () -> String?,
    ): CopyEntryFieldResult {
        val result = when (
            val authorization = authorizationGate.authorize(
                AuthorizationScope.Global(AuthenticationPurpose.COPY_SECRET),
            ) {
                val code = codeProvider()?.takeIf { it.isNotBlank() && '-' !in it }
                    ?: return@authorize CopyEntryFieldResult.Unavailable
                clipboardWriter.writeSensitive(code)
                CopyEntryFieldResult.Copied
            }
        ) {
            is AuthorizationResult.Allowed -> authorization.value
            is AuthorizationResult.Denied,
            AuthorizationResult.Cancelled,
            -> CopyEntryFieldResult.NotAuthorized
        }
        if (result == CopyEntryFieldResult.Copied) {
            activityRecorder.recordUsage(entryId.value, ActivityType.COPY_PASSWORD)
        }
        return result
    }
}

internal sealed interface CopyEntryFieldResult {
    data object Copied : CopyEntryFieldResult
    data object NotAuthorized : CopyEntryFieldResult
    data object Unavailable : CopyEntryFieldResult
}
