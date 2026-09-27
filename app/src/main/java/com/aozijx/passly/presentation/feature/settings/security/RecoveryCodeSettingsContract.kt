package com.aozijx.passly.presentation.feature.settings.security

import com.aozijx.passly.domain.sensitive.EmptySensitiveValue
import com.aozijx.passly.domain.sensitive.SensitiveValue

enum class RecoveryCodeDraftStatus {
    EMPTY,
    CREATING,
    READY,
    EXPIRED,
    COMMITTED,
    FAILED,
}

data class RecoveryCodeSettingsUiState(
    val hasRecoveryCode: Boolean = false,
    val draftStatus: RecoveryCodeDraftStatus = RecoveryCodeDraftStatus.EMPTY,
    val disclosure: SensitiveValue = EmptySensitiveValue,
    val verificationInput: SensitiveValue = EmptySensitiveValue,
    val isVerifying: Boolean = false,
    val verificationResult: Boolean? = null,
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
