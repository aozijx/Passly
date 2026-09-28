package com.aozijx.passly.feature.vault.history

import com.aozijx.passly.domain.access.model.AuthorizationResult
import com.aozijx.passly.domain.access.model.AuthorizationScope
import com.aozijx.passly.domain.access.model.SensitiveRevisionAccessAction
import com.aozijx.passly.domain.access.port.AuthorizationGate
import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.entry.model.history.EntryRevisionId
import com.aozijx.passly.domain.entry.model.sensitive.RevealedRevisionSensitiveField
import com.aozijx.passly.domain.entry.model.sensitive.SensitiveFieldKey
import com.aozijx.passly.domain.entry.port.EntryRevisionRepository
import javax.inject.Inject

internal class RevealRevisionFieldsUseCase @Inject constructor(
    private val repository: EntryRevisionRepository,
    private val authorizationGate: AuthorizationGate,
) {
    suspend operator fun invoke(
        entryId: EntryId,
        revisionId: EntryRevisionId,
        keys: Set<SensitiveFieldKey>,
    ): RevisionOperationResult<List<RevealedRevisionSensitiveField>> {
        if (keys.isEmpty()) return RevisionOperationResult.Succeeded(emptyList())
        val scope = AuthorizationScope.SensitiveRevision(
            entryId = entryId,
            revisionId = revisionId.value,
            fieldKeys = keys,
            action = SensitiveRevisionAccessAction.REVEAL,
        )
        return when (val result = authorizationGate.authorize(scope) { permit ->
            repository.reveal(entryId, revisionId, keys, permit)
        }) {
            is AuthorizationResult.Allowed -> result.value.toRevisionOperationResult()
            is AuthorizationResult.Denied -> RevisionOperationResult.AuthenticationDenied(result.failure)
            AuthorizationResult.Cancelled -> RevisionOperationResult.Cancelled
        }
    }
}
