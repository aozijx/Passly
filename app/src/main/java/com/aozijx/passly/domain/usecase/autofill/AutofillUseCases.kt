package com.aozijx.passly.domain.usecase.autofill

import com.aozijx.passly.core.error.AppResult
import com.aozijx.passly.domain.model.activity.ActivityType
import com.aozijx.passly.domain.repository.autofill.AutofillStatusRepository
import com.aozijx.passly.domain.repository.vault.ActivityRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

data class AutofillUseCases @Inject constructor(
    val checkStatus: CheckAutofillStatusUseCase,
    val observeStatus: ObserveAutofillStatusUseCase,
    val isSupported: IsAutofillSupportedUseCase,
    val openSettings: OpenAutofillSettingsUseCase,
    val recordUsage: RecordAutofillUsageUseCase,
    val saveCredential: SaveAutofillCredentialUseCase
)

@Singleton
class CheckAutofillStatusUseCase @Inject constructor(
    private val repository: AutofillStatusRepository
) {
    operator fun invoke(): Boolean = repository.isAutofillServiceEnabled()
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
    operator fun invoke(): Boolean = repository.isAutofillSupported()
}

@Singleton
class OpenAutofillSettingsUseCase @Inject constructor(
    private val repository: AutofillStatusRepository
) {
    operator fun invoke() = repository.openAutofillSettings()
}

@Singleton
class RecordAutofillUsageUseCase @Inject constructor(
    private val activityRepository: ActivityRepository
) {
    suspend operator fun invoke(candidateId: Int): AppResult<Unit> =
        activityRepository.record(candidateId.toString(), ActivityType.AUTOFILL)
}

@Singleton
class SaveAutofillCredentialUseCase @Inject constructor() {
    suspend operator fun invoke(
        packageName: String?,
        webDomain: String?,
        pageTitle: String?,
        usernameValue: String,
        passwordValue: String
    ): AppResult<Long> {
        return AppResult.runSuspendCatching("save_autofill_credential") {
            TODO("Implement autofill credential saving")
        }
    }
}
