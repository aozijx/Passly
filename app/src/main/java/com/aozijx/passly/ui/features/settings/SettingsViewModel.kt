package com.aozijx.passly.ui.features.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aozijx.passly.core.auth.validation.AuthRequestValidator
import com.aozijx.passly.domain.model.SwipeActionType
import com.aozijx.passly.domain.usecase.auth.AuthUseCases
import com.aozijx.passly.domain.usecase.settings.system.SystemSettingsUseCases
import com.aozijx.passly.ui.features.verification.internal.VerificationCoordinator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    authUseCases: AuthUseCases,
    authRequestValidator: AuthRequestValidator,
    private val systemSettingsUseCases: SystemSettingsUseCases
) : ViewModel() {
    val authGateway = VerificationCoordinator(viewModelScope, authUseCases, authRequestValidator)
    val isAppPasswordEnabled: StateFlow<Boolean> = authGateway.isAppPasswordEnabled

    fun setSwipeLeftAction(action: SwipeActionType) {
        viewModelScope.launch { systemSettingsUseCases.setSwipeLeftAction(action) }
    }

    fun setSwipeRightAction(action: SwipeActionType) {
        viewModelScope.launch { systemSettingsUseCases.setSwipeRightAction(action) }
    }
}