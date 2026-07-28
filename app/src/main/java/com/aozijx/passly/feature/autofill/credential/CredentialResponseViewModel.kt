package com.aozijx.passly.feature.autofill.credential

import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.credentials.CreatePasswordRequest
import androidx.credentials.GetPasswordOption
import androidx.credentials.exceptions.CreateCredentialCancellationException
import androidx.credentials.exceptions.CreateCredentialException
import androidx.credentials.exceptions.CreateCredentialUnknownException
import androidx.credentials.exceptions.CreateCredentialUnsupportedException
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.GetCredentialUnknownException
import androidx.credentials.exceptions.GetCredentialUnsupportedException
import androidx.credentials.exceptions.NoCredentialException
import androidx.credentials.provider.PendingIntentHandler
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aozijx.passly.app.diagnostics.AppTelemetry
import com.aozijx.passly.domain.authentication.AuthenticationManager
import com.aozijx.passly.domain.authentication.AuthenticationPurpose
import com.aozijx.passly.domain.authentication.AuthenticationRequest
import com.aozijx.passly.domain.authentication.AuthenticationResult
import com.aozijx.passly.domain.autofill.usecase.CreatePasswordCredentialResult
import com.aozijx.passly.domain.autofill.usecase.CredentialResponseUseCases
import com.aozijx.passly.domain.autofill.usecase.PasswordCredentialResult
import com.aozijx.passly.service.autofill.credential.CredentialBeginGetHandler
import com.aozijx.passly.service.autofill.credential.CredentialCallingAppResolver
import com.aozijx.passly.service.autofill.credential.CredentialResponseFactory
import com.aozijx.passly.service.autofill.credential.ModernCredentialService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
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
        data class Complete(val resultIntent: Intent) : UiState()
        object Unrecoverable : UiState()
    }

    private val _state = MutableStateFlow<UiState>(UiState.Loading)
    val state: StateFlow<UiState> = _state.asStateFlow()
    private val requestStarted = AtomicBoolean(false)

    fun handlePasswordGet(sourceIntent: Intent) {
        if (!requestStarted.compareAndSet(false, true)) return
        viewModelScope.launch {
            AppTelemetry.i(TAG, "Password credential request received")

            try {
                val providerRequest =
                    PendingIntentHandler.retrieveProviderGetCredentialRequest(sourceIntent)
                        ?: return@launch completeGetError(
                            GetCredentialUnknownException("Missing system get request")
                        )
                val passwordOption =
                    providerRequest.credentialOptions.singleOrNull() as? GetPasswordOption
                        ?: return@launch completeGetError(
                            GetCredentialUnsupportedException(
                                "Selected entry is not a password request"
                            )
                        )
                val packageName =
                    CredentialCallingAppResolver.resolveNativePackage(
                        providerRequest.callingAppInfo
                    ) ?: return@launch completeGetError(
                        GetCredentialUnsupportedException(
                            "Privileged origin requests are not configured"
                        )
                    )
                val entryId =
                    sourceIntent.getStringExtra(ModernCredentialService.EXTRA_ENTRY_ID)
                        .orEmpty()
                if (entryId.isBlank()) {
                    return@launch completeGetError(
                        GetCredentialUnknownException("Missing selected credential id")
                    )
                }

                when (
                    val result = useCase.resolvePasswordCredential(
                        entryId,
                        packageName,
                        null,
                        passwordOption.allowedUserIds,
                    )
                ) {
                    is PasswordCredentialResult.Success -> {
                        val intent = CredentialResponseFactory.buildPasswordResponse(
                            result.username, result.password
                        )
                        _state.value = UiState.Complete(intent)
                        AppTelemetry.i(TAG, "Password credential resolved")
                    }

                    is PasswordCredentialResult.NotFound -> {
                        AppTelemetry.w(TAG, "Password credential not found")
                        completeGetError(NoCredentialException("Credential is no longer available"))
                    }

                    is PasswordCredentialResult.NotAuthorized -> {
                        AppTelemetry.i(TAG, "Password credential authorization did not complete")
                        completeGetError(getAuthenticationException(result.authentication))
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                AppTelemetry.e(TAG, "Password credential resolution failed", e)
                completeGetError(GetCredentialUnknownException("Credential resolution failed"))
            }
        }
    }

    fun handleUnlock(intent: Intent) {
        if (!requestStarted.compareAndSet(false, true)) return
        viewModelScope.launch {
            try {
                val request = PendingIntentHandler.retrieveBeginGetCredentialRequest(intent)
                    ?: return@launch completeGetError(
                        GetCredentialUnknownException("Missing system begin-get request")
                    )
                if (
                    CredentialCallingAppResolver.resolveNativePackage(request.callingAppInfo) == null
                ) {
                    return@launch completeGetError(
                        GetCredentialUnsupportedException(
                            "Privileged origin requests are not configured"
                        )
                    )
                }
                val authentication = authenticationManager.authenticate(
                    AuthenticationRequest(AuthenticationPurpose.AUTOFILL)
                )
                if (authentication !is AuthenticationResult.Success) {
                    completeGetError(getAuthenticationException(authentication))
                    return@launch
                }
                val response = beginGetHandler.resolve(
                    request = request,
                    context = appContext,
                    includeUnlockAction = false,
                )
                val result = Intent()
                PendingIntentHandler.setBeginGetCredentialResponse(result, response)
                _state.value = UiState.Complete(result)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                AppTelemetry.e(TAG, "Credential unlock failed", e)
                completeGetError(GetCredentialUnknownException("Credential unlock failed"))
            }
        }
    }

    fun handlePasswordCreate(sourceIntent: Intent) {
        if (!requestStarted.compareAndSet(false, true)) return
        viewModelScope.launch {
            try {
                val providerRequest =
                    PendingIntentHandler.retrieveProviderCreateCredentialRequest(sourceIntent)
                        ?: return@launch completeCreateError(
                            CreateCredentialUnknownException("Missing system create request")
                        )
                val createRequest = providerRequest.callingRequest as? CreatePasswordRequest
                    ?: return@launch completeCreateError(
                        CreateCredentialUnsupportedException(
                            "Selected entry is not a password create request"
                        )
                    )
                val packageName =
                    CredentialCallingAppResolver.resolveNativePackage(
                        providerRequest.callingAppInfo
                    ) ?: return@launch completeCreateError(
                        CreateCredentialUnsupportedException(
                            "Privileged origin requests are not configured"
                        )
                    )

                when (
                    val result = useCase.createPasswordCredential(
                        packageName = packageName,
                        username = createRequest.id,
                        password = createRequest.password,
                    )
                ) {
                    CreatePasswordCredentialResult.Success -> {
                        _state.value = UiState.Complete(
                            CredentialResponseFactory.buildPasswordCreateResponse()
                        )
                        AppTelemetry.i(TAG, "Password credential created")
                    }

                    CreatePasswordCredentialResult.NotSaved -> {
                        completeCreateError(
                            CreateCredentialUnknownException("Credential was not saved")
                        )
                    }

                    is CreatePasswordCredentialResult.NotAuthorized -> {
                        completeCreateError(
                            createAuthenticationException(result.authentication)
                        )
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                AppTelemetry.e(TAG, "Password credential creation failed", e)
                completeCreateError(
                    CreateCredentialUnknownException("Credential creation failed")
                )
            }
        }
    }

    fun rejectUnknownAction() {
        if (requestStarted.compareAndSet(false, true)) {
            _state.value = UiState.Unrecoverable
        }
    }

    private fun completeGetError(exception: GetCredentialException) {
        _state.value = UiState.Complete(
            CredentialResponseFactory.buildGetException(exception)
        )
    }

    private fun completeCreateError(
        exception: CreateCredentialException,
    ) {
        _state.value = UiState.Complete(
            CredentialResponseFactory.buildCreateException(exception)
        )
    }

    private fun getAuthenticationException(
        authentication: AuthenticationResult,
    ): GetCredentialException = when (authentication) {
        is AuthenticationResult.Cancelled ->
            GetCredentialCancellationException("Credential access was cancelled")

        is AuthenticationResult.Failure ->
            GetCredentialUnknownException("Credential authentication failed")

        is AuthenticationResult.Success ->
            GetCredentialUnknownException("Unexpected authentication result")
    }

    private fun createAuthenticationException(
        authentication: AuthenticationResult,
    ): CreateCredentialException = when (authentication) {
        is AuthenticationResult.Cancelled ->
            CreateCredentialCancellationException("Credential creation was cancelled")

        is AuthenticationResult.Failure ->
            CreateCredentialUnknownException("Credential authentication failed")

        is AuthenticationResult.Success ->
            CreateCredentialUnknownException("Unexpected authentication result")
    }

    companion object {
        private const val TAG = "CredResponse"
    }
}
