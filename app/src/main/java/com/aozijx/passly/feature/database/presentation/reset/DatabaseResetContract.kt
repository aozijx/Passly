package com.aozijx.passly.feature.database.presentation.reset

import androidx.annotation.StringRes
import com.aozijx.passly.R
import com.aozijx.passly.presentation.shared.error.UiErrorMessage

sealed interface DatabaseResetUiState {
    @get:StringRes
    val titleRes: Int?
    val isButtonEnabled: Boolean
    val isLoading: Boolean

    data object Idle : DatabaseResetUiState {
        override val titleRes = R.string.database_reset_action
        override val isButtonEnabled = true
        override val isLoading = false
    }

    data object Resetting : DatabaseResetUiState {
        override val titleRes = null
        override val isButtonEnabled = false
        override val isLoading = true
    }

    data object Completed : DatabaseResetUiState {
        override val titleRes = R.string.database_reset_success
        override val isButtonEnabled = false
        override val isLoading = false
    }

    data class Failed(
        val reason: DatabaseResetFailure,
    ) : DatabaseResetUiState {
        @get:StringRes
        override val titleRes = reason.titleRes
        override val isButtonEnabled = false
        override val isLoading = false
    }
}

sealed interface DatabaseResetFailure {
    data object SessionRestricted : DatabaseResetFailure

    data object AuthorizationDenied : DatabaseResetFailure

    data class Unexpected(
        val message: UiErrorMessage,
    ) : DatabaseResetFailure
}

private val DatabaseResetFailure.titleRes: Int
    @StringRes
    get() =
        when (this) {
            DatabaseResetFailure.SessionRestricted -> R.string.database_reset_error_session_restricted
            DatabaseResetFailure.AuthorizationDenied -> R.string.database_reset_error_authorization_denied
            is DatabaseResetFailure.Unexpected -> message.resourceId
        }

sealed interface DatabaseResetUiAction {
    data object Reset : DatabaseResetUiAction
}

internal sealed interface DatabaseResetMutation {
    data object PageBoundaryCrossed : DatabaseResetMutation

    data object ResetStarted : DatabaseResetMutation

    data object ResetCancelled : DatabaseResetMutation

    data object ResetCompleted : DatabaseResetMutation

    data object ResetSuccessFeedbackElapsed : DatabaseResetMutation

    data class ResetFailed(
        val reason: DatabaseResetFailure,
    ) : DatabaseResetMutation
}
