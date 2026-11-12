package com.aozijx.passly.feature.vault.entry

import com.aozijx.passly.domain.access.model.AuthenticationPurpose
import com.aozijx.passly.domain.access.model.AuthorizationPermit
import com.aozijx.passly.domain.access.model.AuthorizationResult
import com.aozijx.passly.domain.access.model.AuthorizationScope
import com.aozijx.passly.domain.access.model.SensitiveAccessAction
import com.aozijx.passly.domain.access.port.AuthorizationGate
import com.aozijx.passly.domain.clipboard.port.SensitiveClipboardWriter
import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.model.FieldKey
import com.aozijx.passly.domain.entry.model.activity.ActivityType
import com.aozijx.passly.domain.entry.model.sensitive.SensitiveFieldKey
import com.aozijx.passly.domain.entry.policy.EntryFieldReader
import com.aozijx.passly.domain.entry.policy.EntryTypeDefinitions
import com.aozijx.passly.domain.entry.port.ActivityRecorder
import com.aozijx.passly.domain.entry.port.EntryQueryRepository
import com.aozijx.passly.domain.entry.port.SensitiveFieldRepository
import com.aozijx.passly.feature.vault.copy.SensitiveCopyResult
import com.aozijx.passly.feature.vault.copy.sensitiveCopyDeliveryGuard
import javax.inject.Inject

internal class CopyEntryFieldUseCase
    @Inject
    constructor(
        private val authorizationGate: AuthorizationGate,
        private val entryQueryRepository: EntryQueryRepository,
        private val sensitiveFieldRepository: SensitiveFieldRepository,
        private val clipboardWriter: SensitiveClipboardWriter,
        private val activityRecorder: ActivityRecorder,
    ) {
        suspend operator fun invoke(
            entryId: EntryId,
            entryType: EntryType,
            fieldKey: FieldKey,
            canDeliver: () -> Boolean,
        ): SensitiveCopyResult {
            val canWrite = sensitiveCopyDeliveryGuard(canDeliver)
            val sensitiveKey = EntryTypeDefinitions.sensitiveStorageKey(entryType, fieldKey)
            val scope =
                sensitiveKey?.let {
                    AuthorizationScope.SensitiveFields(
                        entryId = entryId,
                        fieldKeys = setOf(it),
                        action = SensitiveAccessAction.COPY,
                    )
                } ?: AuthorizationScope.Global(AuthenticationPurpose.COPY_SECRET)

            val result =
                when (
                    val authorization =
                        authorizationGate.authorize(scope) { permit ->
                            if (sensitiveKey == null) {
                                if (!canWrite()) return@authorize SensitiveCopyResult.Unavailable
                                val entry =
                                    entryQueryRepository.getById(entryId)
                                        ?: return@authorize SensitiveCopyResult.Unavailable
                                if (!canWrite()) return@authorize SensitiveCopyResult.Unavailable
                                val value =
                                    EntryFieldReader
                                        .getFieldValue(entry, fieldKey)
                                        ?.takeIf(String::isNotBlank)
                                        ?: return@authorize SensitiveCopyResult.Unavailable
                                if (clipboardWriter.writeSensitiveIf(value, canWrite)) {
                                    SensitiveCopyResult.Copied
                                } else {
                                    SensitiveCopyResult.Unavailable
                                }
                            } else {
                                copySensitiveField(entryId, sensitiveKey, permit, canWrite)
                            }
                        }
                ) {
                    is AuthorizationResult.Allowed -> authorization.value

                    is AuthorizationResult.Denied,
                    AuthorizationResult.Cancelled,
                    -> SensitiveCopyResult.NotAuthorized
                }
            if (result == SensitiveCopyResult.Copied && canWrite()) {
                activityRecorder.recordUsage(
                    entryId = entryId.value,
                    type = ActivityType.COPY_FIELD,
                    affectedFields = setOf(fieldKey),
                )
            }
            return result
        }

        private suspend fun copySensitiveField(
            entryId: EntryId,
            key: SensitiveFieldKey,
            permit: AuthorizationPermit,
            canWrite: () -> Boolean,
        ): SensitiveCopyResult {
            val revealed =
                sensitiveFieldRepository.reveal(
                    entryId = entryId,
                    key = key,
                    action = SensitiveAccessAction.COPY,
                    permit = permit,
                ) ?: return SensitiveCopyResult.Unavailable
            return try {
                if (!canWrite()) return SensitiveCopyResult.Unavailable
                val chars = revealed.value.toCharArray()
                try {
                    if (chars.isEmpty()) {
                        SensitiveCopyResult.Unavailable
                    } else {
                        if (clipboardWriter.writeSensitiveIf(String(chars), canWrite)) {
                            SensitiveCopyResult.Copied
                        } else {
                            SensitiveCopyResult.Unavailable
                        }
                    }
                } finally {
                    chars.fill('\u0000')
                }
            } finally {
                revealed.value.wipe()
            }
        }
    }
