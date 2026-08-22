package com.aozijx.passly.feature.autofill.shared

import com.aozijx.passly.core.error.result.AppResult
import com.aozijx.passly.domain.autofill.port.AutofillStatusRepository
import com.aozijx.passly.domain.autofill.port.CredentialServiceRepository
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
    ) {
        val policy = settingsRepository.settings.first().interaction.autofill
        check(policy.enabled && policy.savePromptsEnabled) {
            "Autofill save prompts are disabled"
        }
        check(
            repository.save(
                packageName = packageName,
                webDomain = webDomain,
                pageTitle = pageTitle,
                usernameValue = usernameValue,
                passwordValue = passwordValue,
            )
        ) { "Failed to save credential" }
    }
}
