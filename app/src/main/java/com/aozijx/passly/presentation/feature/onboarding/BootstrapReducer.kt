package com.aozijx.passly.presentation.feature.onboarding

import com.aozijx.passly.domain.access.model.AuthenticationFailure
import com.aozijx.passly.domain.sensitive.EmptySensitiveValue
import com.aozijx.passly.domain.sensitive.SensitiveValue
import com.aozijx.passly.presentation.feature.onboarding.BootstrapUiState

internal sealed interface BootstrapMutation {
    data class NewAppPasswordChanged(val value: SensitiveValue) : BootstrapMutation
    data class ConfirmAppPasswordChanged(val value: SensitiveValue) : BootstrapMutation
    data class SetPasswordDialogVisibilityChanged(val visible: Boolean) : BootstrapMutation
    data object SetupStarted : BootstrapMutation
    data class SetupFailed(val failure: AuthenticationFailure) : BootstrapMutation
    data object SetupFinished : BootstrapMutation
    data object SetupCompleted : BootstrapMutation
}

internal object BootstrapReducer {
    fun reduce(
        state: BootstrapUiState,
        mutation: BootstrapMutation,
    ): BootstrapUiState = when (mutation) {
        is BootstrapMutation.NewAppPasswordChanged -> state.copy(
            newAppPassword = mutation.value,
            setupFailure = null,
        )
        is BootstrapMutation.ConfirmAppPasswordChanged -> state.copy(
            confirmAppPassword = mutation.value,
            setupFailure = null,
        )
        is BootstrapMutation.SetPasswordDialogVisibilityChanged -> state.copy(
            showSetPasswordDialog = mutation.visible,
            newAppPassword = if (mutation.visible) state.newAppPassword else EmptySensitiveValue,
            confirmAppPassword = if (mutation.visible) {
                state.confirmAppPassword
            } else {
                EmptySensitiveValue
            },
            setupFailure = null,
        )
        BootstrapMutation.SetupStarted -> state.copy(
            isSettingAppPassword = true,
            setupFailure = null,
        )
        is BootstrapMutation.SetupFailed -> state.copy(setupFailure = mutation.failure)
        BootstrapMutation.SetupFinished -> state.copy(isSettingAppPassword = false)
        BootstrapMutation.SetupCompleted -> state.copy(
            showSetPasswordDialog = false,
            newAppPassword = EmptySensitiveValue,
            confirmAppPassword = EmptySensitiveValue,
            setupFailure = null,
        )
    }
}
