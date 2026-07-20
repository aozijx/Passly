package com.aozijx.passly.domain.usecase.autofill

import com.aozijx.passly.core.error.AppResult
import com.aozijx.passly.domain.model.activity.ActivityType
import com.aozijx.passly.domain.repository.autofill.AutofillStatusRepository
import com.aozijx.passly.domain.repository.autofill.CredentialServiceRepository
import com.aozijx.passly.domain.repository.entry.RecordEntryUsageFacade
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CheckAutofillStatusUseCase @Inject constructor(
    private val repository: AutofillStatusRepository
) {
    suspend operator fun invoke() = repository.isAutofillServiceEnabled()
}

@Singleton
class ObserveAutofillStatusUseCase @Inject constructor(
    private val repository: AutofillStatusRepository
) {
    operator fun invoke(): Flow<Boolean> = repository.observeAutofillStatus()
}

@Singleton
class IsAutofillSupportedUseCase @Inject constructor(
    private val repository: AutofillStatusRepository
) {
    operator fun invoke() = repository.isAutofillSupported()
}

@Singleton
class OpenAutofillSettingsUseCase @Inject constructor(
    private val repository: AutofillStatusRepository
) {
    operator fun invoke() = repository.openAutofillSettings()
}

@Singleton
class RecordAutofillUsageUseCase @Inject constructor(
    private val recordEntryUsageFacade: RecordEntryUsageFacade
) {
    suspend operator fun invoke(candidateId: Int): AppResult<Unit> =
        recordEntryUsageFacade.record(candidateId.toString(), ActivityType.AUTOFILL)
}

@Singleton
class SaveAutofillCredentialUseCase @Inject constructor(
    private val repository: CredentialServiceRepository
) {
    suspend operator fun invoke(
        packageName: String?,
        webDomain: String?,
        pageTitle: String?,
        usernameValue: String,
        passwordValue: String
    ): AppResult<Unit> {
        return AppResult.runSuspendCatching("save_autofill_credential") {
            val success = repository.save(
                packageName = packageName,
                webDomain = webDomain,
                pageTitle = pageTitle,
                usernameValue = usernameValue,
                passwordValue = passwordValue
            )
            if (!success) throw IllegalStateException("Failed to save credential")
        }
    }
}

data class AutofillUseCases @Inject constructor(
    val checkStatus: CheckAutofillStatusUseCase,
    val observeStatus: ObserveAutofillStatusUseCase,
    val isSupported: IsAutofillSupportedUseCase,
    val openSettings: OpenAutofillSettingsUseCase,
    val recordUsage: RecordAutofillUsageUseCase,
    val saveCredential: SaveAutofillCredentialUseCase
)
