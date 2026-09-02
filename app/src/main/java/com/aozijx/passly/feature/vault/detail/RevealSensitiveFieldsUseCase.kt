package com.aozijx.passly.feature.vault.detail

import com.aozijx.passly.domain.access.model.AuthorizationResult
import com.aozijx.passly.domain.access.model.AuthorizationScope
import com.aozijx.passly.domain.access.model.SensitiveAccessAction
import com.aozijx.passly.domain.access.port.AuthorizationGate
import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.entry.model.activity.ActivityType
import com.aozijx.passly.domain.entry.model.sensitive.SensitiveFieldKey
import com.aozijx.passly.domain.entry.port.ActivityRecorder
import com.aozijx.passly.domain.entry.port.SensitiveFieldRepository
import com.aozijx.passly.domain.sensitive.SensitiveValue

internal class RevealSensitiveFieldsUseCase(
    private val authorizationGate: AuthorizationGate,
    private val sensitiveFieldRepository: SensitiveFieldRepository,
    private val activityRecorder: ActivityRecorder,
) {
    suspend fun reveal(
        entryId: EntryId,
        requestedFields: Set<SensitiveFieldKey>,
    ): Map<SensitiveFieldKey, SensitiveValue> {
        if (requestedFields.isEmpty()) return emptyMap()
        val scope = AuthorizationScope.SensitiveFields(
            entryId = entryId,
            fieldKeys = requestedFields,
            action = SensitiveAccessAction.REVEAL,
        )
        return when (
            val authorization = authorizationGate.authorize(scope) { permit ->
                sensitiveFieldRepository.revealMany(
                    entryId = entryId,
                    keys = scope.fieldKeys,
                    permit = permit,
                )
            }
        ) {
            is AuthorizationResult.Allowed -> {
                val values = authorization.value.mapNotNull { revealed ->
                    revealed.key.takeIf(requestedFields::contains)?.let { it to revealed.value }
                }.toMap()
                if (values.isNotEmpty()) {
                    activityRecorder.recordUsage(entryId.value, ActivityType.VIEW)
                }
                values
            }

            is AuthorizationResult.Denied,
            AuthorizationResult.Cancelled,
            -> emptyMap()
        }
    }
}
