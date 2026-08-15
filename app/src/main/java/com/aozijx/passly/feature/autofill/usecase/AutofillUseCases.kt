package com.aozijx.passly.feature.autofill.usecase

import com.aozijx.passly.core.error.result.AppResult
import com.aozijx.passly.data.repository.autofill.AutofillStatusRepository
import com.aozijx.passly.data.repository.autofill.CredentialServiceRepository
import com.aozijx.passly.domain.entry.model.activity.ActivityType
import com.aozijx.passly.domain.entry.port.ActivityRecorder
import com.aozijx.passly.domain.settings.port.AppSettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
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
    private val activityRecorder: ActivityRecorder
) {
    suspend operator fun invoke(candidateId: String): AppResult<Unit> =
        activityRecorder.recordUsage(candidateId, ActivityType.AUTOFILL)
}

@Singleton
class SaveAutofillCredentialUseCase @Inject constructor(
    private val repository: CredentialServiceRepository,
    private val settingsRepository: AppSettingsRepository,
) {
    suspend operator fun invoke(
        packageName: String?,
        webDomain: String?,
        pageTitle: String?,
        usernameValue: String,
        passwordValue: String
    ): AppResult<Unit> {
        return AppResult.runSuspendCatching {
            val policy = settingsRepository.settings.first().interaction.autofill
            if (!policy.enabled || !policy.savePromptsEnabled) {
                throw IllegalStateException("Autofill save prompts are disabled")
            }
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
