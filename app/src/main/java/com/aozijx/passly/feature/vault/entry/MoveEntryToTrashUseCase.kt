package com.aozijx.passly.feature.vault.entry

import com.aozijx.passly.core.error.model.NotFound
import com.aozijx.passly.core.error.model.SessionModeRestricted
import com.aozijx.passly.core.error.result.AppResult
import com.aozijx.passly.domain.access.port.SecureSessionAccessState
import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.entry.port.EntryCommandRepository
import com.aozijx.passly.domain.entry.port.EntryQueryRepository
import com.aozijx.passly.feature.vault.otp.OtpCodeInvalidator

internal class MoveEntryToTrashUseCase(
    private val entryCommandRepository: EntryCommandRepository,
    private val entryQueryRepository: EntryQueryRepository,
    private val secureSessionAccessState: SecureSessionAccessState,
    private val otpCodeInvalidator: OtpCodeInvalidator,
) {
    suspend operator fun invoke(entryId: EntryId): AppResult<Unit> {
        if (!secureSessionAccessState.hasFullSecureSessionAccess()) {
            return AppResult.Failure(SessionModeRestricted())
        }
        val entry = entryQueryRepository.getById(entryId)
            ?: return AppResult.Failure(NotFound())

        return entryCommandRepository.moveToTrash(entry.id, entry.version).onSuccess {
            otpCodeInvalidator.entryRemoved(entry.id.value)
        }
    }
}
