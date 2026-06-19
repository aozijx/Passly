package com.aozijx.passly.ui.features.settings

import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aozijx.passly.core.auth.validation.AuthRequestValidator
import com.aozijx.passly.core.error.AppResult
import com.aozijx.passly.domain.config.UserConfigProvider
import com.aozijx.passly.domain.usecase.auth.AuthUseCases
import com.aozijx.passly.ui.features.settings.state.SettingsUiState
import com.aozijx.passly.ui.features.verification.internal.VerificationCoordinator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    authUseCases: AuthUseCases,
    authRequestValidator: AuthRequestValidator,
    val configProvider: UserConfigProvider
) : ViewModel() {

    val authGateway = VerificationCoordinator(viewModelScope, authUseCases, authRequestValidator)

    val config: StateFlow<SettingsUiState> = configProvider.config

    val isAppPasswordEnabled: StateFlow<Boolean> = authGateway.isAppPasswordEnabled

    fun switchKeyInvalidationPolicy(
        activity: FragmentActivity,
        enabled: Boolean,
        onResult: (AppResult<Unit>) -> Unit
    ) {
        authGateway.rekeyWithInvalidationPolicy(activity, enabled, onResult)
    }
}