package com.aozijx.passly.feature.vault.entry

import javax.inject.Inject

import com.aozijx.passly.core.error.model.AppError
import com.aozijx.passly.core.error.model.NotFound
import com.aozijx.passly.core.error.model.SessionModeRestricted
import com.aozijx.passly.core.error.result.AppResult
import com.aozijx.passly.domain.access.model.AuthenticationPurpose
import com.aozijx.passly.domain.access.model.AuthorizationResult
import com.aozijx.passly.domain.access.model.AuthorizationScope
import com.aozijx.passly.domain.access.port.AuthorizationGate
import com.aozijx.passly.domain.access.port.SecureSessionAccessState
import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.entry.port.EntryCommandRepository
import com.aozijx.passly.domain.entry.port.EntryQueryRepository

internal class MoveEntryToTrashUseCase @Inject constructor(
    private val entryCommandRepository: EntryCommandRepository,
    private val entryQueryRepository: EntryQueryRepository,
    private val secureSessionAccessState: SecureSessionAccessState,
    private val authorizationGate: AuthorizationGate,
) {
    suspend operator fun invoke(entryId: EntryId): MoveEntryToTrashResult {
        if (!secureSessionAccessState.hasFullSecureSessionAccess()) {
            return MoveEntryToTrashResult.Failed(SessionModeRestricted())
        }

        return when (
            val authorization = authorizationGate.authorize(
                AuthorizationScope.Global(AuthenticationPurpose.DELETE_ENTRY),
            ) {
                val entry = entryQueryRepository.getById(entryId)
                    ?: return@authorize MoveEntryToTrashResult.Failed(NotFound())
                when (val result = entryCommandRepository.moveToTrash(entry.id, entry.version)) {
                    is AppResult.Success -> {
                        MoveEntryToTrashResult.Moved
                    }

                    is AppResult.Failure -> MoveEntryToTrashResult.Failed(result.error)
                }
            }
        ) {
            is AuthorizationResult.Allowed -> authorization.value
            is AuthorizationResult.Cancelled,
            is AuthorizationResult.Denied -> MoveEntryToTrashResult.NotAuthorized
        }
    }
}

internal sealed interface MoveEntryToTrashResult {
    data object Moved : MoveEntryToTrashResult
    data object NotAuthorized : MoveEntryToTrashResult
    data class Failed(val error: AppError) : MoveEntryToTrashResult
}
