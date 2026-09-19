package com.aozijx.passly.feature.vault.trash

import com.aozijx.passly.core.error.model.AppError
import com.aozijx.passly.core.error.model.SessionModeRestricted
import com.aozijx.passly.core.error.result.AppResult
import com.aozijx.passly.domain.access.model.AuthenticationPurpose
import com.aozijx.passly.domain.access.model.AuthorizationResult
import com.aozijx.passly.domain.access.model.AuthorizationScope
import com.aozijx.passly.domain.access.port.AuthorizationGate
import com.aozijx.passly.domain.access.port.SecureSessionAccessState
import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.entry.model.EntryVersion
import com.aozijx.passly.domain.entry.port.EntryCommandRepository
import javax.inject.Inject

internal class TrashCommandUseCase @Inject constructor(
    private val entryCommandRepository: EntryCommandRepository,
    private val secureSessionAccessState: SecureSessionAccessState,
    private val authorizationGate: AuthorizationGate,
) {
    suspend fun restore(
        entryId: EntryId,
        expectedVersion: EntryVersion,
    ): TrashCommandResult {
        val restriction = sessionRestriction()
        if (restriction != null) return restriction
        return entryCommandRepository.restoreEntry(entryId, expectedVersion).toTrashCommandResult()
    }

    suspend fun deletePermanently(
        entryId: EntryId,
        expectedVersion: EntryVersion,
    ): TrashCommandResult = authorizeDestructiveCommand {
        entryCommandRepository.deletePermanently(entryId, expectedVersion)
            .toTrashCommandResult()
    }

    suspend fun empty(): TrashCommandResult = authorizeDestructiveCommand {
        entryCommandRepository.emptyTrash().toTrashCommandResult()
    }

    private suspend fun authorizeDestructiveCommand(
        command: suspend () -> TrashCommandResult,
    ): TrashCommandResult {
        val restriction = sessionRestriction()
        if (restriction != null) return restriction
        return when (
            val authorization = authorizationGate.authorize(
                AuthorizationScope.Global(AuthenticationPurpose.DELETE_ENTRY),
            ) {
                command()
            }
        ) {
            is AuthorizationResult.Allowed -> authorization.value
            AuthorizationResult.Cancelled,
            is AuthorizationResult.Denied -> TrashCommandResult.NotAuthorized
        }
    }

    private fun sessionRestriction(): TrashCommandResult.Failed? =
        if (secureSessionAccessState.hasFullSecureSessionAccess()) {
            null
        } else {
            TrashCommandResult.Failed(SessionModeRestricted())
        }
}

internal sealed interface TrashCommandResult {
    data object Completed : TrashCommandResult
    data object NotAuthorized : TrashCommandResult
    data class Failed(val error: AppError) : TrashCommandResult
}

private fun AppResult<*>.toTrashCommandResult(): TrashCommandResult = when (this) {
    is AppResult.Success -> TrashCommandResult.Completed
    is AppResult.Failure -> TrashCommandResult.Failed(error)
}
