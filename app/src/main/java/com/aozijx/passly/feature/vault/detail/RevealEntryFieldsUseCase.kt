package com.aozijx.passly.feature.vault.detail

import javax.inject.Inject

import com.aozijx.passly.domain.access.model.AuthorizationResult
import com.aozijx.passly.domain.access.model.AuthorizationScope
import com.aozijx.passly.domain.access.model.SensitiveAccessAction
import com.aozijx.passly.domain.access.port.AuthorizationGate
import com.aozijx.passly.domain.entry.model.Entry
import com.aozijx.passly.domain.entry.model.EntryFieldAccess
import com.aozijx.passly.domain.entry.model.FieldKey
import com.aozijx.passly.domain.entry.model.activity.ActivityType
import com.aozijx.passly.domain.entry.policy.EntryFieldReader
import com.aozijx.passly.domain.entry.policy.EntryTypeDefinitions
import com.aozijx.passly.domain.entry.port.ActivityRecorder
import com.aozijx.passly.domain.entry.port.SensitiveFieldRepository
import com.aozijx.passly.domain.sensitive.OwnedChars
import com.aozijx.passly.domain.sensitive.SensitiveValue

internal class RevealEntryFieldsUseCase @Inject constructor(
    private val authorizationGate: AuthorizationGate,
    private val entryFieldReader: EntryFieldReader,
    private val sensitiveFieldRepository: SensitiveFieldRepository,
    private val activityRecorder: ActivityRecorder,
) {
    suspend fun reveal(
        entry: Entry,
        requestedFields: Set<FieldKey>,
        recordLowSensitivityAccess: Boolean,
    ): Map<FieldKey, SensitiveValue> {
        if (requestedFields.isEmpty()) return emptyMap()
        val definition = EntryTypeDefinitions[entry.type]
        val values = linkedMapOf<FieldKey, SensitiveValue>()

        requestedFields.forEach { fieldKey ->
            if (definition[fieldKey]?.access == EntryFieldAccess.SECRET) {
                OwnedChars.fromNullableString(entryFieldReader.getFieldValue(entry, fieldKey))
                    ?.let { values[fieldKey] = it }
            }
        }

        val highSensitivityFields = requestedFields.mapNotNull { fieldKey ->
            definition[fieldKey]
                ?.takeIf { it.access == EntryFieldAccess.HIGH_SENSITIVITY }
                ?.sensitiveFieldKey
                ?.let { it to fieldKey }
        }.toMap()
        if (highSensitivityFields.isNotEmpty()) {
            val scope = AuthorizationScope.SensitiveFields(
                entryId = entry.id,
                fieldKeys = highSensitivityFields.keys,
                action = SensitiveAccessAction.REVEAL,
            )
            when (
                val authorization = authorizationGate.authorize(scope) { permit ->
                    sensitiveFieldRepository.revealMany(
                        entryId = entry.id,
                        keys = scope.fieldKeys,
                        action = SensitiveAccessAction.REVEAL,
                        permit = permit,
                    )
                }
            ) {
                is AuthorizationResult.Allowed -> authorization.value.forEach { revealed ->
                    highSensitivityFields[revealed.key]?.let { values[it] = revealed.value }
                }
                is AuthorizationResult.Denied,
                AuthorizationResult.Cancelled,
                -> Unit
            }
        }

        val revealedHighSensitivity = values.keys.any { fieldKey ->
            definition[fieldKey]?.access == EntryFieldAccess.HIGH_SENSITIVITY
        }
        val revealedLowSensitivity = values.keys.any { fieldKey ->
            definition[fieldKey]?.access == EntryFieldAccess.SECRET
        }
        if (revealedHighSensitivity || (recordLowSensitivityAccess && revealedLowSensitivity)) {
            activityRecorder.recordUsage(entry.id.value, ActivityType.VIEW)
        }
        return values
    }
}