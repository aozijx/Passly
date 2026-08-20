package com.aozijx.passly.feature.autofill.credential

import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.credentials.exceptions.CreateCredentialException
import androidx.credentials.exceptions.CreateCredentialUnknownException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.GetCredentialUnknownException
import androidx.credentials.exceptions.NoCredentialException
import androidx.credentials.provider.PendingIntentHandler
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aozijx.passly.app.diagnostics.AppTelemetry
import com.aozijx.passly.domain.access.model.AuthenticationResult
import com.aozijx.passly.domain.autofill.model.AutofillGrantContext
import com.aozijx.passly.domain.autofill.port.AutofillGrantStore
import com.aozijx.passly.feature.autofill.credential.service.CredentialBeginGetHandler
import com.aozijx.passly.feature.autofill.credential.service.CredentialCallingAppResolver
import com.aozijx.passly.feature.autofill.credential.service.CredentialResponseFactory
import com.aozijx.passly.feature.autofill.shared.AutofillRequestSession
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
    private val interactor: CredentialResponseInteractor,
    private val beginGetHandler: CredentialBeginGetHandler,
    private val requestSession: AutofillRequestSession,
    private val grantStore: AutofillGrantStore,
    @param:ApplicationContext private val appContext: android.content.Context,
) : ViewModel() {

    private val _state = MutableStateFlow<CredentialResponseUiState>(
        CredentialResponseUiState.Loading
    )
    val state: StateFlow<CredentialResponseUiState> = _state.asStateFlow()
    private val requestStarted = AtomicBoolean(false)

    fun onIntent(intent: CredentialResponseIntent) {
        when (intent) {
            is CredentialResponseIntent.PasswordGet -> handlePasswordGet(intent.sourceIntent)
            is CredentialResponseIntent.Unlock -> handleUnlock(intent.sourceIntent)
            is CredentialResponseIntent.PasswordCreate -> handlePasswordCreate(intent.sourceIntent)
            CredentialResponseIntent.UnknownAction -> rejectUnknownAction()
        }
    }

    private fun handlePasswordGet(sourceIntent: Intent) {
        if (!requestStarted.compareAndSet(false, true)) return
        viewModelScope.launch {
            AppTelemetry.i(TAG, "Password credential request received")

            try {
                val request = when (val parsed = CredentialRequestParser.parsePasswordGet(
                    sourceIntent
                )) {
                    is PasswordGetParseResult.Ready -> parsed
                    is PasswordGetParseResult.Failed -> {
                        completeGetError(parsed.exception)
                        return@launch
                    }
                }

                when (val result = requestSession.trackUnlock {
                    interactor.resolvePasswordCredential(
                        request.entryId,
                        request.packageName,
                        null,
                        request.option.allowedUserIds,
                    )
                }) {
                    is PasswordCredentialResult.Success -> {
                        val intent = CredentialResponseFactory.buildPasswordResponse(
                            result.username, result.password
                        )
                        mutate(CredentialResponseMutation.Completed(intent))
                        AppTelemetry.i(TAG, "Password credential resolved")
                    }

                    is PasswordCredentialResult.NotFound -> {
                        AppTelemetry.w(TAG, "Password credential not found")
                        completeGetError(NoCredentialException("Credential is no longer available"))
                    }

                    is PasswordCredentialResult.NotAuthorized -> {
                        AppTelemetry.i(TAG, "Password credential authorization did not complete")
                        completeGetError(
                            CredentialAuthenticationExceptionMapper.toGetException(
                                result.authentication
                            )
                        )
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

    private fun handleUnlock(intent: Intent) {
        if (!requestStarted.compareAndSet(false, true)) return
        viewModelScope.launch {
            try {
                val request = when (val parsed = CredentialRequestParser.parseUnlock(intent)) {
                    is UnlockParseResult.Ready -> parsed.request
                    is UnlockParseResult.Failed -> {
                        completeGetError(parsed.exception)
                        return@launch
                    }
                }
                val authentication = requestSession.authenticate()
                if (authentication !is AuthenticationResult.Success) {
                    completeGetError(
                        CredentialAuthenticationExceptionMapper.toGetException(authentication)
                    )
                    return@launch
                }
                // 解锁动作成功后授予短期会话授权，用户随即选择条目进入
                // ACTION_GET_PASSWORD 时不再重复弹认证。
                CredentialCallingAppResolver.resolveNativePackage(request.callingAppInfo)
                    ?.let { packageName ->
                        grantStore.grant(
                            AutofillGrantContext(packageName = packageName, webDomain = null)
                        )
                    }
                val response = beginGetHandler.resolve(
                    request = request,
                    context = appContext,
                    includeUnlockAction = false,
                )
                val result = Intent()
                PendingIntentHandler.setBeginGetCredentialResponse(result, response)
                mutate(CredentialResponseMutation.Completed(result))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                AppTelemetry.e(TAG, "Credential unlock failed", e)
                completeGetError(GetCredentialUnknownException("Credential unlock failed"))
            }
        }
    }

    private fun handlePasswordCreate(sourceIntent: Intent) {
        if (!requestStarted.compareAndSet(false, true)) return
        viewModelScope.launch {
            try {
                val request = when (val parsed = CredentialRequestParser.parsePasswordCreate(
                    sourceIntent
                )) {
                    is PasswordCreateParseResult.Ready -> parsed
                    is PasswordCreateParseResult.Failed -> {
                        completeCreateError(parsed.exception)
                        return@launch
                    }
                }

                when (val result = requestSession.trackUnlock {
                    interactor.createPasswordCredential(
                        packageName = request.packageName,
                        username = request.username,
                        password = request.password,
                    )
                }) {
                    CreatePasswordCredentialResult.Success -> {
                        mutate(
                            CredentialResponseMutation.Completed(
                                CredentialResponseFactory.buildPasswordCreateResponse()
                            )
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
                            CredentialAuthenticationExceptionMapper.toCreateException(
                                result.authentication
                            )
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

    private fun rejectUnknownAction() {
        if (requestStarted.compareAndSet(false, true)) {
            mutate(CredentialResponseMutation.Unrecoverable)
        }
    }

    suspend fun closeRequestSession() {
        requestSession.close()
    }

    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch {
            requestSession.close()
        }
    }

    private fun completeGetError(exception: GetCredentialException) {
        mutate(
            CredentialResponseMutation.Completed(
                CredentialResponseFactory.buildGetException(exception)
            )
        )
    }

    private fun completeCreateError(
        exception: CreateCredentialException,
    ) {
        mutate(
            CredentialResponseMutation.Completed(
                CredentialResponseFactory.buildCreateException(exception)
            )
        )
    }

    private fun mutate(mutation: CredentialResponseMutation) {
        _state.value = CredentialResponseReducer.reduce(_state.value, mutation)
    }

    companion object {
        private const val TAG = "CredResponse"
    }
}
