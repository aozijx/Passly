package com.aozijx.passly.ui.features.settings

import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aozijx.passly.core.auth.validation.AuthRequestValidator
import com.aozijx.passly.core.error.AppResult
import com.aozijx.passly.domain.usecase.auth.AuthUseCases
import com.aozijx.passly.ui.features.verification.internal.VerificationCoordinator
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    authUseCases: AuthUseCases,
    authRequestValidator: AuthRequestValidator
) : ViewModel() {

    val authGateway = VerificationCoordinator(viewModelScope, authUseCases, authRequestValidator)

    fun switchKeyInvalidationPolicy(
        activity: FragmentActivity,
        enabled: Boolean,
        onResult: (AppResult<Unit>) -> Unit
    ) {
        authGateway.rekeyWithInvalidationPolicy(activity, enabled, onResult)
    }
}