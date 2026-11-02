package com.aozijx.passly.security.authentication.host

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import com.aozijx.passly.core.observability.LogStatements
import com.aozijx.passly.core.observability.Logger
import com.aozijx.passly.core.observability.Operations
import com.aozijx.passly.core.observability.Reasons
import com.aozijx.passly.domain.access.model.AuthenticationMethod
import com.aozijx.passly.domain.access.model.AuthenticationPurpose
import com.aozijx.passly.security.R
import com.aozijx.passly.security.authentication.SecretChars
import kotlin.coroutines.resume
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine

internal sealed interface AuthenticationDialogRequest {
    val identity: AuthUiRequestId

    data class ChooseMethod(
        val purpose: AuthenticationPurpose,
        val methods: List<AuthenticationMethod>,
        val continuation: CancellableContinuation<AuthenticationMethod?>,
        override val identity: AuthUiRequestId = AuthUiRequestId(),
    ) : AuthenticationDialogRequest

    data class Secret(
        val purpose: AuthenticationPurpose,
        val method: AuthenticationMethod,
        val continuation: CancellableContinuation<SecretHostResult>,
        override val identity: AuthUiRequestId = AuthUiRequestId(),
    ) : AuthenticationDialogRequest
}

internal fun submitSecretInput(
    request: AuthenticationDialogRequest?,
    expectedIdentity: AuthUiRequestId,
    chars: CharArray?,
) {
    val activeRequest =
        when (request) {
            is AuthenticationDialogRequest.Secret -> {
                request.takeIf {
                    it.identity === expectedIdentity && it.continuation.isActive
                }
            }

            is AuthenticationDialogRequest.ChooseMethod, null -> {
                null
            }
        }
    if (activeRequest == null) {
        chars?.fill('\u0000')
        return
    }
    activeRequest.continuation.resume(
        if (chars == null) {
            SecretHostResult.Cancelled(true)
        } else {
            SecretHostResult.Submitted(SecretChars.take(chars))
        },
    )
}

class ActivityAuthUiHost(
    private val activity: FragmentActivity,
    override val ownerId: String,
    private val logger: Logger,
) : AuthUiHost {
    private val dialogState = MutableStateFlow<AuthenticationDialogRequest?>(null)
    internal val dialog: StateFlow<AuthenticationDialogRequest?> = dialogState.asStateFlow()
    private val activePrompt = RequestOwnedSlot<ActiveBiometricPrompt>()

    override fun snapshot(): AuthHostSnapshot =
        AuthHostSnapshot(
            resumed = activity.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED),
            finishing = activity.isFinishing,
            destroyed = activity.isDestroyed,
        )

    override suspend fun chooseMethod(
        purpose: AuthenticationPurpose,
        methods: List<AuthenticationMethod>,
    ): AuthenticationMethod? {
        if (!snapshot().usable) return null
        return suspendCancellableCoroutine { continuation ->
            val request = AuthenticationDialogRequest.ChooseMethod(purpose, methods, continuation)
            if (!dialogState.compareAndSet(null, request)) {
                continuation.resume(null)
                return@suspendCancellableCoroutine
            }
            continuation.invokeOnCancellation {
                dialogState.compareAndSet(request, null)
            }
        }
    }

    override suspend fun requestSecret(
        purpose: AuthenticationPurpose,
        method: AuthenticationMethod,
    ): SecretHostResult {
        if (!snapshot().usable) return SecretHostResult.HostUnavailable
        return suspendCancellableCoroutine { continuation ->
            val request = AuthenticationDialogRequest.Secret(purpose, method, continuation)
            if (!dialogState.compareAndSet(null, request)) {
                continuation.resume(SecretHostResult.HostUnavailable)
                return@suspendCancellableCoroutine
            }
            continuation.invokeOnCancellation {
                dialogState.compareAndSet(request, null)
            }
        }
    }

    override suspend fun authenticateBiometric(
        spec: BiometricPromptSpec,
        cryptoObject: BiometricPrompt.CryptoObject?,
    ): BiometricHostResult =
        logger.inSpan(Operations.SecurityAuthentication) {
            if (!snapshot().usable) return@inSpan BiometricHostResult.HostUnavailable
            val promptInfo =
                try {
                    BiometricPromptSpecFactory.create(
                        context = activity,
                        spec = spec,
                        cryptoBound = cryptoObject != null,
                    )
                } catch (failure: IllegalArgumentException) {
                    logger.log(LogStatements.Security.biometricPromptFailed(failure, Reasons.InvalidConfiguration))
                    return@inSpan BiometricHostResult.Failure(BiometricHostFailure.CRYPTO_OBJECT_INVALID)
                }
            suspendCancellableCoroutine { continuation ->
                val requestIdentity = AuthUiRequestId()
                val callback =
                    object : BiometricPrompt.AuthenticationCallback() {
                        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                            val activeRequest = activePrompt.take(requestIdentity) ?: return
                            if (activeRequest.continuation.isActive) {
                                activeRequest.continuation.resume(
                                    BiometricHostResult.Success(
                                        result,
                                    ),
                                )
                            }
                        }

                        override fun onAuthenticationError(
                            errorCode: Int,
                            errString: CharSequence,
                        ) {
                            val activeRequest = activePrompt.take(requestIdentity) ?: return
                            if (!activeRequest.continuation.isActive) return
                            val result =
                                when (errorCode) {
                                    BiometricPrompt.ERROR_NEGATIVE_BUTTON,
                                    BiometricPrompt.ERROR_USER_CANCELED,
                                    -> {
                                        BiometricHostResult.Cancelled(true)
                                    }

                                    BiometricPrompt.ERROR_CANCELED,
                                    BiometricPrompt.ERROR_TIMEOUT,
                                    -> {
                                        BiometricHostResult.Cancelled(false)
                                    }

                                    else -> {
                                        BiometricHostResult.Failure(
                                            BiometricPromptErrorClassifier.classify(errorCode),
                                            errorCode,
                                        )
                                    }
                                }
                            activeRequest.continuation.resume(result)
                        }
                    }
                val prompt =
                    try {
                        BiometricPrompt(activity, activity.mainExecutor, callback)
                    } catch (failure: IllegalStateException) {
                        logger.log(
                            LogStatements.Security.biometricPromptFailed(
                                failure,
                                Reasons.HostUnavailable,
                                degraded = true,
                            ),
                        )
                        if (continuation.isActive) continuation.resume(BiometricHostResult.HostUnavailable)
                        return@suspendCancellableCoroutine
                    }
                val activeRequest = ActiveBiometricPrompt(continuation) { prompt.cancelAuthentication() }
                if (!activePrompt.install(requestIdentity, activeRequest)) {
                    if (continuation.isActive) continuation.resume(BiometricHostResult.HostUnavailable)
                    return@suspendCancellableCoroutine
                }
                continuation.invokeOnCancellation {
                    activePrompt.take(requestIdentity)?.cancelPrompt()
                }
                if (!snapshot().usable) {
                    if (activePrompt.take(requestIdentity) != null) {
                        activeRequest.cancelPrompt()
                        if (continuation.isActive) continuation.resume(BiometricHostResult.HostUnavailable)
                    }
                    return@suspendCancellableCoroutine
                }
                try {
                    val started =
                        activeRequest.startIfActive {
                            if (cryptoObject == null) {
                                prompt.authenticate(promptInfo)
                            } else {
                                prompt.authenticate(promptInfo, cryptoObject)
                            }
                        }
                    if (!started) return@suspendCancellableCoroutine
                } catch (failure: IllegalArgumentException) {
                    if (activePrompt.take(requestIdentity) == null) return@suspendCancellableCoroutine
                    activeRequest.cancelPrompt()
                    logger.log(LogStatements.Security.biometricPromptFailed(failure, Reasons.InvalidConfiguration))
                    if (continuation.isActive) {
                        continuation.resume(
                            BiometricHostResult.Failure(BiometricHostFailure.CRYPTO_OBJECT_INVALID),
                        )
                    }
                } catch (failure: IllegalStateException) {
                    if (activePrompt.take(requestIdentity) == null) return@suspendCancellableCoroutine
                    activeRequest.cancelPrompt()
                    logger.log(
                        LogStatements.Security.biometricPromptFailed(
                            failure,
                            Reasons.HostUnavailable,
                            degraded = true,
                        ),
                    )
                    if (continuation.isActive) continuation.resume(BiometricHostResult.HostUnavailable)
                } catch (failure: SecurityException) {
                    if (activePrompt.take(requestIdentity) == null) return@suspendCancellableCoroutine
                    activeRequest.cancelPrompt()
                    logger.log(LogStatements.Security.biometricPromptFailed(failure, Reasons.PermissionDenied))
                    if (continuation.isActive) {
                        continuation.resume(
                            BiometricHostResult.Failure(BiometricHostFailure.METHOD_UNAVAILABLE),
                        )
                    }
                } catch (failure: Exception) {
                    if (activePrompt.take(requestIdentity) == null) return@suspendCancellableCoroutine
                    activeRequest.cancelPrompt()
                    logger.log(LogStatements.Security.biometricPromptFailed(failure, Reasons.UnknownFailure))
                    if (continuation.isActive) continuation.resume(BiometricHostResult.HostUnavailable)
                }
            }
        }

    internal fun submitMethod(
        identity: AuthUiRequestId,
        method: AuthenticationMethod?,
    ) {
        val request = dialogState.value as? AuthenticationDialogRequest.ChooseMethod ?: return
        if (request.identity !== identity) return
        if (!dialogState.compareAndSet(request, null)) return
        if (request.continuation.isActive) request.continuation.resume(method)
    }

    internal fun submitSecret(
        identity: AuthUiRequestId,
        chars: CharArray?,
    ) {
        val request = dialogState.value
        if (request?.identity !== identity || request !is AuthenticationDialogRequest.Secret) {
            chars?.fill('\u0000')
            return
        }
        if (!dialogState.compareAndSet(request, null)) {
            chars?.fill('\u0000')
            return
        }
        submitSecretInput(request, identity, chars)
    }

    fun cancelOwnedRequests() {
        activePrompt.takeCurrent()?.cancel()
        val request = dialogState.value
        when (request) {
            is AuthenticationDialogRequest.ChooseMethod -> request.continuation.cancel()
            is AuthenticationDialogRequest.Secret -> request.continuation.cancel()
            null -> Unit
        }
        if (request != null) dialogState.compareAndSet(request, null)
    }
}

internal object BiometricPromptSpecFactory {
    fun create(
        context: Context,
        spec: BiometricPromptSpec,
        cryptoBound: Boolean,
    ): BiometricPrompt.PromptInfo {
        require(!cryptoBound || !spec.allowDeviceCredential) {
            "A biometric-only keystore operation cannot allow device credentials"
        }
        val authenticators =
            if (spec.allowDeviceCredential) {
                BiometricManager.Authenticators.BIOMETRIC_STRONG or
                    BiometricManager.Authenticators.DEVICE_CREDENTIAL
            } else {
                BiometricManager.Authenticators.BIOMETRIC_STRONG
            }
        return BiometricPrompt.PromptInfo
            .Builder()
            .setTitle(context.getString(titleResource(spec.purpose)))
            .setSubtitle(context.getString(R.string.security_auth_biometric_subtitle))
            .setAllowedAuthenticators(authenticators)
            .apply {
                if (!spec.allowDeviceCredential) {
                    setNegativeButtonText(context.getString(R.string.security_auth_cancel))
                }
            }.build()
    }

    private fun titleResource(purpose: AuthenticationPurpose): Int =
        when (purpose) {
            AuthenticationPurpose.UNLOCK_VAULT -> {
                R.string.security_auth_purpose_unlock_vault
            }

            AuthenticationPurpose.AUTOFILL -> {
                R.string.security_auth_purpose_autofill
            }

            AuthenticationPurpose.BACKUP_EXPORT -> {
                R.string.security_auth_purpose_backup_export
            }

            AuthenticationPurpose.BACKUP_IMPORT -> {
                R.string.security_auth_purpose_backup_import
            }

            AuthenticationPurpose.EXPORT_DIAGNOSTICS -> {
                R.string.security_auth_purpose_export_diagnostics
            }

            AuthenticationPurpose.RECOVER_AUTH_METHODS -> {
                R.string.security_auth_purpose_recover_auth_methods
            }

            AuthenticationPurpose.CLEAR_DATABASE -> {
                R.string.security_auth_purpose_clear_database
            }

            else -> {
                R.string.security_auth_verify_identity
            }
        }
}

internal object BiometricPromptErrorClassifier {
    fun classify(errorCode: Int): BiometricHostFailure =
        when (errorCode) {
            BiometricPrompt.ERROR_LOCKOUT,
            BiometricPrompt.ERROR_LOCKOUT_PERMANENT,
            -> BiometricHostFailure.RATE_LIMITED

            BiometricPrompt.ERROR_HW_NOT_PRESENT,
            BiometricPrompt.ERROR_HW_UNAVAILABLE,
            BiometricPrompt.ERROR_NO_BIOMETRICS,
            BiometricPrompt.ERROR_NO_DEVICE_CREDENTIAL,
            BiometricPrompt.ERROR_SECURITY_UPDATE_REQUIRED,
            -> BiometricHostFailure.METHOD_UNAVAILABLE

            BiometricPrompt.ERROR_UNABLE_TO_PROCESS,
            BiometricPrompt.ERROR_NO_SPACE,
            BiometricPrompt.ERROR_VENDOR,
            -> BiometricHostFailure.CRYPTO_OBJECT_INVALID

            else -> BiometricHostFailure.AUTHENTICATION_FAILED
        }
}
