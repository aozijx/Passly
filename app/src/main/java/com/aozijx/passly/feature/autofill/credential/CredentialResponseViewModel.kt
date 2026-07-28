package com.aozijx.passly.feature.autofill.credential

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.annotation.RequiresApi
import androidx.credentials.provider.PendingIntentHandler
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aozijx.passly.app.diagnostics.AppTelemetry
import com.aozijx.passly.domain.authentication.AuthenticationManager
import com.aozijx.passly.domain.authentication.AuthenticationPurpose
import com.aozijx.passly.domain.authentication.AuthenticationRequest
import com.aozijx.passly.domain.authentication.AuthenticationResult
import com.aozijx.passly.domain.autofill.usecase.CredentialResponseUseCases
import com.aozijx.passly.domain.autofill.usecase.PasswordCredentialResult
import com.aozijx.passly.service.autofill.credential.CredentialBeginGetHandler
import com.aozijx.passly.service.autofill.credential.CredentialResponseFactory
import com.aozijx.passly.service.autofill.credential.ModernCredentialService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
class CredentialResponseViewModel @Inject constructor(
    private val useCase: CredentialResponseUseCases,
    private val authenticationManager: AuthenticationManager,
    private val beginGetHandler: CredentialBeginGetHandler,
    @param:ApplicationContext private val appContext: android.content.Context,
) : ViewModel() {

    sealed class UiState {
        object Loading : UiState()
        data class Success(val resultIntent: Intent) : UiState()
        object Error : UiState()
    }

    private val _state = MutableStateFlow<UiState>(UiState.Loading)
    val state: StateFlow<UiState> = _state.asStateFlow()

    fun handlePasswordGet(credentialData: Bundle) {
        viewModelScope.launch {
            val entryId =
                credentialData.getString(ModernCredentialService.EXTRA_ENTRY_ID).orEmpty()
            val packageName =
                credentialData.getString(ModernCredentialService.EXTRA_PACKAGE_NAME) ?: ""

            AppTelemetry.i(TAG, "Password credential request received")

            try {
                when (
                    val result = useCase.resolvePasswordCredential(
                        entryId,
                        packageName,
                        null,
                    )
                ) {
                    is PasswordCredentialResult.Success -> {
                        val intent = CredentialResponseFactory.buildPasswordResponse(
                            result.username, result.password
                        )
                        _state.value = UiState.Success(intent)
                        AppTelemetry.i(TAG, "Password credential resolved")
                    }

                    is PasswordCredentialResult.NotFound -> {
                        AppTelemetry.w(TAG, "Password credential not found")
                        _state.value = UiState.Error
                    }

                    is PasswordCredentialResult.NotAuthorized -> {
                        AppTelemetry.i(TAG, "Password credential authorization cancelled")
                        _state.value = UiState.Error
                    }
                }
            } catch (e: Exception) {
                AppTelemetry.e(TAG, "Password credential resolution failed", e)
                _state.value = UiState.Error
            }
        }
    }

    fun handleUnlock(intent: Intent) {
        viewModelScope.launch {
            try {
                val authentication = authenticationManager.authenticate(
                    AuthenticationRequest(AuthenticationPurpose.AUTOFILL)
                )
                if (authentication !is AuthenticationResult.Success) {
                    _state.value = UiState.Error
                    return@launch
                }
                val request = PendingIntentHandler.retrieveBeginGetCredentialRequest(intent)
                    ?: run {
                        _state.value = UiState.Error
                        return@launch
                    }
                val response = beginGetHandler.resolve(
                    request = request,
                    context = appContext,
                    includeUnlockAction = false,
                )
                val result = Intent()
                PendingIntentHandler.setBeginGetCredentialResponse(result, response)
                _state.value = UiState.Success(result)
            } catch (e: Exception) {
                AppTelemetry.e(TAG, "Credential unlock failed", e)
                _state.value = UiState.Error
            }
        }
    }

    companion object {
        private const val TAG = "CredResponse"
    }
}
