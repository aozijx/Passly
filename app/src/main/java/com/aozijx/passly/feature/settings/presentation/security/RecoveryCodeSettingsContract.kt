package com.aozijx.passly.feature.settings.presentation.security

import com.aozijx.passly.domain.sensitive.EmptySensitiveValue
import com.aozijx.passly.domain.sensitive.SensitiveValue

sealed interface RecoveryCodeDraftUiState {
    data object Empty : RecoveryCodeDraftUiState
    data object Creating : RecoveryCodeDraftUiState
    data class Ready(val disclosure: SensitiveValue) : RecoveryCodeDraftUiState
    data class Committing(val disclosure: SensitiveValue) : RecoveryCodeDraftUiState
    data object Expired : RecoveryCodeDraftUiState
    data object Committed : RecoveryCodeDraftUiState
    data object Failed : RecoveryCodeDraftUiState
}

sealed interface RecoveryCodeVerificationUiState {
    val input: SensitiveValue

    data class Editing(override val input: SensitiveValue) : RecoveryCodeVerificationUiState
    data class Verifying(override val input: SensitiveValue) : RecoveryCodeVerificationUiState
    data class Accepted(override val input: SensitiveValue) : RecoveryCodeVerificationUiState
    data class Rejected(override val input: SensitiveValue) : RecoveryCodeVerificationUiState
    data class Failed(override val input: SensitiveValue) : RecoveryCodeVerificationUiState
}

data class RecoveryCodeSettingsUiState(
    val hasRecoveryCode: Boolean = false,
    val draft: RecoveryCodeDraftUiState = RecoveryCodeDraftUiState.Empty,
    val verification: RecoveryCodeVerificationUiState =
        RecoveryCodeVerificationUiState.Editing(EmptySensitiveValue),
)

sealed interface RecoveryCodeSettingsAction {
    data object Generate : RecoveryCodeSettingsAction
    data object Copy : RecoveryCodeSettingsAction
    data object ConfirmAndEnable : RecoveryCodeSettingsAction
    data object DismissDisclosure : RecoveryCodeSettingsAction
    data class VerificationInputChanged(val value: String) : RecoveryCodeSettingsAction
    data object Verify : RecoveryCodeSettingsAction
    data object ClearVerificationResult : RecoveryCodeSettingsAction
}

sealed interface RecoveryCodeSettingsEffect {
    data object Copied : RecoveryCodeSettingsEffect
}
