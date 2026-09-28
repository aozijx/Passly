package com.aozijx.passly.feature.vault.history

import com.aozijx.passly.core.error.result.AppResult
import com.aozijx.passly.domain.access.model.AuthorizationResult
import com.aozijx.passly.domain.access.model.AuthorizationScope
import com.aozijx.passly.domain.access.model.SensitiveRevisionAccessAction
import com.aozijx.passly.domain.access.port.AuthorizationGate
import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.entry.model.EntryVersion
import com.aozijx.passly.domain.entry.model.history.EntryRevisionId
import com.aozijx.passly.domain.entry.policy.SensitiveRevisionRestorePolicy
import com.aozijx.passly.domain.entry.port.EntryQueryRepository
import com.aozijx.passly.domain.entry.port.EntryRevisionRepository
import com.aozijx.passly.domain.entry.port.SensitiveFieldRepository
import javax.inject.Inject

internal class RestoreEntryRevisionUseCase @Inject constructor(
    private val revisionRepository: EntryRevisionRepository,
    private val entryRepository: EntryQueryRepository,
    private val sensitiveFieldRepository: SensitiveFieldRepository,
    private val authorizationGate: AuthorizationGate,
) {
    suspend operator fun invoke(
        entryId: EntryId,
        revisionId: EntryRevisionId,
    ): RevisionOperationResult<EntryVersion> {
        val current = entryRepository.getById(entryId)
            ?: return RevisionOperationResult.Missing
        val historical = revisionRepository.loadRedacted(entryId, revisionId)
        val snapshot = when (historical) {
            is AppResult.Success -> historical.data
            is AppResult.Failure -> return historical.error.toRevisionOperationFailure()
        }
        val currentKeys = sensitiveFieldRepository.getPresence(entryId).keys
        val affectedKeys = SensitiveRevisionRestorePolicy.affectedFields(
            currentFields = currentKeys,
            historicalFields = snapshot.sensitiveFieldKeys,
        )
        if (affectedKeys.isEmpty()) {
            return revisionRepository.restore(entryId, revisionId, current.version, null)
                .toRevisionOperationResult()
        }
        val scope = AuthorizationScope.SensitiveRevision(
            entryId = entryId,
            revisionId = revisionId.value,
            fieldKeys = affectedKeys,
            action = SensitiveRevisionAccessAction.RESTORE,
        )
        return when (val result = authorizationGate.authorize(scope) { permit ->
            revisionRepository.restore(entryId, revisionId, current.version, permit)
        }) {
            is AuthorizationResult.Allowed -> result.value.toRevisionOperationResult()
            is AuthorizationResult.Denied -> RevisionOperationResult.AuthenticationDenied(result.failure)
            AuthorizationResult.Cancelled -> RevisionOperationResult.Cancelled
        }
    }

}
