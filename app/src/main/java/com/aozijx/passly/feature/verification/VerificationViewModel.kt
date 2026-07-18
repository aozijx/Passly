package com.aozijx.passly.feature.verification

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aozijx.passly.domain.authentication.AuthenticationCallback
import com.aozijx.passly.domain.authentication.AuthenticationManager
import com.aozijx.passly.domain.authentication.AuthenticationMethod
import com.aozijx.passly.domain.authentication.AuthenticationMethodProvisioner
import com.aozijx.passly.domain.authentication.AuthenticationPurpose
import com.aozijx.passly.domain.authentication.AuthenticationRequest
import com.aozijx.passly.domain.authentication.AuthenticationResult
import com.aozijx.passly.domain.authentication.AuthenticationState
import com.aozijx.passly.feature.verification.model.VerificationUiState
import com.aozijx.passly.security.authentication.AuthFeedbackPresenter
import com.github.f4b6a3.uuid.UuidCreator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class VerificationViewModel @Inject constructor(
    private val authenticationManager: AuthenticationManager,
    private val methodProvisioner: AuthenticationMethodProvisioner,
    private val feedback: AuthFeedbackPresenter
) : ViewModel() {

    val state: StateFlow<AuthenticationState> = authenticationManager.state
    val methodAvailability = authenticationManager.methods

    private val _uiState = MutableStateFlow(VerificationUiState())
    val uiState: StateFlow<VerificationUiState> = _uiState.asStateFlow()

    fun verifyWithBiometric() = authenticate(AuthenticationMethod.BIOMETRIC)
    fun verifyWithAppPassword() = authenticate(AuthenticationMethod.APP_PASSWORD)
    fun unlockWithRecoveryCode() = authenticate(AuthenticationMethod.RECOVERY_CODE)

    fun onShowSetPasswordDialog() = _uiState.update { it.copy(showSetPasswordDialog = true) }
    fun onDismissSetPasswordDialog() = _uiState.update { it.copy(showSetPasswordDialog = false) }

    fun bootstrapAppPassword(password: CharArray) {
        viewModelScope.launch {
            val result = methodProvisioner.setAppPassword(password)
            feedback.present(result, UuidCreator.getTimeOrderedEpoch().toString())
            if (result is AuthenticationResult.Success) {
                _uiState.update { it.copy(showSetPasswordDialog = false) }
            }
        }
    }

    private fun authenticate(method: AuthenticationMethod) {
        val request = AuthenticationRequest(
            purpose = AuthenticationPurpose.UNLOCK_VAULT,
            allowedMethods = setOf(method)
        )
        authenticationManager.authenticate(request, AuthenticationCallback { _ -> })
    }
}
