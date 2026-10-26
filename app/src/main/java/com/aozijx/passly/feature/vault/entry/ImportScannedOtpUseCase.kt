package com.aozijx.passly.feature.vault.entry

import com.aozijx.passly.core.error.result.AppResult
import com.aozijx.passly.core.observability.LogStatements
import com.aozijx.passly.core.observability.Logger
import com.aozijx.passly.core.observability.Operations
import com.aozijx.passly.domain.access.port.SecureSessionAccessState
import com.aozijx.passly.domain.entry.model.otp.OtpConfig
import com.aozijx.passly.domain.entry.otp.toNewEntryDraft
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

class ImportScannedOtpUseCase @Inject constructor(
    private val createEntry: CreateEntryUseCase,
    private val access: SecureSessionAccessState,
    private val logger: Logger,
) {
    suspend operator fun invoke(config: OtpConfig): Boolean {
        if (!access.hasFullSecureSessionAccess()) return false
        return logger.withRootTrace(Operations.VaultOtpImport) {
            val draft = try {
                config.toNewEntryDraft()
            } catch (error: IllegalArgumentException) {
                logger.log(LogStatements.Application.scannedOtpInvalid(error))
                return@withRootTrace false
            }
            try {
                val result = createEntry(draft)
                currentCoroutineContext().ensureActive()
                result is AppResult.Success
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                // CreateEntryUseCase records unexpected persistence failures in its trace.
                false
            }
        }
    }
}
