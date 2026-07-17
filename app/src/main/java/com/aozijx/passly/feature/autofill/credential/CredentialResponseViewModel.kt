package com.aozijx.passly.feature.autofill.credential

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.annotation.RequiresApi
import androidx.credentials.GetPublicKeyCredentialOption
import androidx.credentials.provider.PendingIntentHandler
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aozijx.passly.core.log.Logcat
import com.aozijx.passly.domain.usecase.credential.CredentialResponseUseCases
import com.aozijx.passly.domain.usecase.credential.PasswordCredentialResult
import com.aozijx.passly.service.autofill.credential.CredentialResponseFactory
import com.aozijx.passly.service.autofill.credential.ModernCredentialService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CredentialResponseViewModel @Inject constructor(
    private val useCase: CredentialResponseUseCases,
) : ViewModel() {

    sealed class UiState {
        object Loading : UiState()
        data class Success(val resultIntent: Intent) : UiState()
        object Error : UiState()
    }

    private val _state = MutableStateFlow<UiState>(UiState.Loading)
    val state: StateFlow<UiState> = _state.asStateFlow()

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    fun handlePasswordGet(credentialData: Bundle) {
        viewModelScope.launch {
            val packageName =
                credentialData.getString(ModernCredentialService.EXTRA_PACKAGE_NAME) ?: ""
            val webDomain = credentialData.getString(ModernCredentialService.EXTRA_WEB_DOMAIN)

            Logcat.i(TAG, "Phase 2: password for pkg=$packageName")

            try {
                when (val result = useCase.resolvePasswordCredential(packageName, webDomain)) {
                    is PasswordCredentialResult.Success -> {
                        val intent = CredentialResponseFactory.buildPasswordResponse(
                            result.username, result.password
                        )
                        _state.value = UiState.Success(intent)
                        Logcat.i(TAG, "Password credential returned for ${result.username}")
                    }

                    is PasswordCredentialResult.NotFound -> {
                        Logcat.w(TAG, "No credential resolved for $packageName")
                        _state.value = UiState.Error
                    }
                }
            } catch (e: Exception) {
                Logcat.e(TAG, "Password credential resolution failed", e)
                _state.value = UiState.Error
            }
        }
    }

    fun handlePasskeyGet(intent: Intent) {
        viewModelScope.launch {
            Logcat.i(TAG, "Phase 2: passkey request")

            try {
                val getRequest = PendingIntentHandler.retrieveProviderGetCredentialRequest(intent)
                val publicKeyOption = getRequest?.credentialOptions
                    ?.firstOrNull() as? GetPublicKeyCredentialOption

                if (publicKeyOption == null) {
                    Logcat.e(TAG, "No public key credential option in request")
                    _state.value = UiState.Error
                    return@launch
                }

                Logcat.w(TAG, "Passkey full signing not yet implemented")

                val result = CredentialResponseFactory.buildPasskeyResponse()
                _state.value = UiState.Success(result)
            } catch (e: Exception) {
                Logcat.e(TAG, "Passkey resolution failed", e)
                _state.value = UiState.Error
            }
        }
    }

    companion object {
        private const val TAG = "CredResponse"
    }
}