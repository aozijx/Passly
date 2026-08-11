package com.aozijx.passly.core.ui.components.auth

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import com.aozijx.passly.R
import com.aozijx.passly.app.diagnostics.AppTelemetry
import com.aozijx.passly.core.telemetry.EventCategory
import com.aozijx.passly.domain.authentication.AuthenticationMethod
import com.aozijx.passly.domain.authentication.AuthenticationPurpose
import com.aozijx.passly.security.authentication.SecretChars
import com.aozijx.passly.security.authentication.host.AuthHostSnapshot
import com.aozijx.passly.security.authentication.host.AuthUiHost
import com.aozijx.passly.security.authentication.host.BiometricHostFailure
import com.aozijx.passly.security.authentication.host.BiometricHostResult
import com.aozijx.passly.security.authentication.host.BiometricPromptSpec
import com.aozijx.passly.security.authentication.host.SecretHostResult
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.resume

internal sealed interface AuthenticationDialogRequest {
    data class ChooseMethod(
        val purpose: AuthenticationPurpose,
        val methods: List<AuthenticationMethod>,
        val continuation: CancellableContinuation<AuthenticationMethod?>
    ) : AuthenticationDialogRequest

    data class Secret(
        val purpose: AuthenticationPurpose,
        val method: AuthenticationMethod,
        val continuation: CancellableContinuation<SecretHostResult>
    ) : AuthenticationDialogRequest
}

class ActivityAuthUiHost(
    private val activity: FragmentActivity,
    override val ownerId: String
) : AuthUiHost {
    private val _dialog = MutableStateFlow<AuthenticationDialogRequest?>(null)
    internal val dialog: StateFlow<AuthenticationDialogRequest?> = _dialog.asStateFlow()
    private val activePrompt = AtomicReference<BiometricPrompt?>(null)

    override fun snapshot(): AuthHostSnapshot = AuthHostSnapshot(
        resumed = activity.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED),
        finishing = activity.isFinishing,
        destroyed = activity.isDestroyed
    )

    override suspend fun chooseMethod(
        purpose: AuthenticationPurpose,
        methods: List<AuthenticationMethod>
    ): AuthenticationMethod? {
        if (!snapshot().usable) return null
        return suspendCancellableCoroutine { continuation ->
            val request = AuthenticationDialogRequest.ChooseMethod(purpose, methods, continuation)
            _dialog.value = request
            continuation.invokeOnCancellation {
                if (_dialog.value === request) _dialog.value = null
            }
        }
    }

    override suspend fun requestSecret(
        purpose: AuthenticationPurpose,
        method: AuthenticationMethod
    ): SecretHostResult {
        if (!snapshot().usable) return SecretHostResult.HostUnavailable
        return suspendCancellableCoroutine { continuation ->
            val request = AuthenticationDialogRequest.Secret(purpose, method, continuation)
            _dialog.value = request
            continuation.invokeOnCancellation {
                if (_dialog.value === request) _dialog.value = null
            }
        }
    }

    override suspend fun authenticateBiometric(
        spec: BiometricPromptSpec,
        cryptoObject: BiometricPrompt.CryptoObject?
    ): BiometricHostResult {
        if (!snapshot().usable) return BiometricHostResult.HostUnavailable
        val promptInfo = try {
            BiometricPromptSpecFactory.create(
                context = activity,
                spec = spec,
                cryptoBound = cryptoObject != null
            )
        } catch (failure: IllegalArgumentException) {
            AppTelemetry.e(
                EventCategory.AUTHENTICATION,
                "biometric.prompt_configuration_invalid",
                throwable = failure
            )
            return BiometricHostResult.Failure(BiometricHostFailure.CRYPTO_OBJECT_INVALID)
        }
        return suspendCancellableCoroutine { continuation ->
            val callback = object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    activePrompt.set(null)
                    if (continuation.isActive) continuation.resume(
                        BiometricHostResult.Success(
                            result
                        )
                    )
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    activePrompt.set(null)
                    if (!continuation.isActive) return
                    val result = when (errorCode) {
                        BiometricPrompt.ERROR_NEGATIVE_BUTTON,
                        BiometricPrompt.ERROR_USER_CANCELED -> BiometricHostResult.Cancelled(true)

                        BiometricPrompt.ERROR_CANCELED,
                        BiometricPrompt.ERROR_TIMEOUT -> BiometricHostResult.Cancelled(false)

                        else -> BiometricHostResult.Failure(
                            BiometricPromptErrorClassifier.classify(errorCode),
                            errorCode
                        )
                    }
                    continuation.resume(result)
                }
            }
            val prompt = try {
                BiometricPrompt(activity, activity.mainExecutor, callback)
            } catch (failure: IllegalStateException) {
                AppTelemetry.w(
                    EventCategory.AUTHENTICATION,
                    "biometric.prompt_host_invalid",
                    throwable = failure
                )
                if (continuation.isActive) continuation.resume(BiometricHostResult.HostUnavailable)
                return@suspendCancellableCoroutine
            }
            activePrompt.set(prompt)
            continuation.invokeOnCancellation {
                activePrompt.getAndSet(null)?.cancelAuthentication()
            }
            if (!snapshot().usable) {
                activePrompt.set(null)
                continuation.resume(BiometricHostResult.HostUnavailable)
                return@suspendCancellableCoroutine
            }
            try {
                if (cryptoObject == null) prompt.authenticate(promptInfo)
                else prompt.authenticate(promptInfo, cryptoObject)
            } catch (failure: IllegalArgumentException) {
                activePrompt.set(null)
                AppTelemetry.e(
                    EventCategory.AUTHENTICATION,
                    "biometric.crypto_or_prompt_invalid",
                    throwable = failure
                )
                if (continuation.isActive) {
                    continuation.resume(
                        BiometricHostResult.Failure(BiometricHostFailure.CRYPTO_OBJECT_INVALID)
                    )
                }
            } catch (failure: IllegalStateException) {
                activePrompt.set(null)
                AppTelemetry.w(
                    EventCategory.AUTHENTICATION,
                    "biometric.prompt_show_host_invalid",
                    throwable = failure
                )
                if (continuation.isActive) continuation.resume(BiometricHostResult.HostUnavailable)
            } catch (failure: SecurityException) {
                activePrompt.set(null)
                AppTelemetry.e(
                    EventCategory.AUTHENTICATION,
                    "biometric.prompt_permission_denied",
                    throwable = failure
                )
                if (continuation.isActive) {
                    continuation.resume(
                        BiometricHostResult.Failure(BiometricHostFailure.METHOD_UNAVAILABLE)
                    )
                }
            } catch (failure: Exception) {
                activePrompt.set(null)
                AppTelemetry.e(
                    EventCategory.AUTHENTICATION,
                    "biometric.prompt_unexpected_failure",
                    throwable = failure
                )
                if (continuation.isActive) continuation.resume(BiometricHostResult.HostUnavailable)
            }
        }
    }

    internal fun submitMethod(method: AuthenticationMethod?) {
        val request = _dialog.value as? AuthenticationDialogRequest.ChooseMethod ?: return
        _dialog.value = null
        if (request.continuation.isActive) request.continuation.resume(method)
    }

    internal fun submitSecret(chars: CharArray?) {
        val request = _dialog.value as? AuthenticationDialogRequest.Secret ?: return
        _dialog.value = null
        if (!request.continuation.isActive) {
            chars?.fill('\u0000')
            return
        }
        request.continuation.resume(
            if (chars == null) SecretHostResult.Cancelled(true)
            else SecretHostResult.Submitted(SecretChars.take(chars))
        )
    }

    fun cancelOwnedRequests() {
        activePrompt.getAndSet(null)?.cancelAuthentication()
        when (val request = _dialog.value) {
            is AuthenticationDialogRequest.ChooseMethod -> request.continuation.cancel()
            is AuthenticationDialogRequest.Secret -> request.continuation.cancel()
            null -> Unit
        }
        _dialog.value = null
    }
}

internal object BiometricPromptSpecFactory {
    fun create(
        context: Context,
        spec: BiometricPromptSpec,
        cryptoBound: Boolean
    ): BiometricPrompt.PromptInfo {
        require(!cryptoBound || !spec.allowDeviceCredential) {
            "A biometric-only keystore operation cannot allow device credentials"
        }
        val authenticators = if (spec.allowDeviceCredential) {
            BiometricManager.Authenticators.BIOMETRIC_STRONG or
                    BiometricManager.Authenticators.DEVICE_CREDENTIAL
        } else {
            BiometricManager.Authenticators.BIOMETRIC_STRONG
        }
        return BiometricPrompt.PromptInfo.Builder()
            .setTitle(context.getString(titleResource(spec.purpose)))
            .setSubtitle(context.getString(R.string.auth_biometric_subtitle))
            .setAllowedAuthenticators(authenticators)
            .apply {
                if (!spec.allowDeviceCredential) {
                    setNegativeButtonText(context.getString(R.string.cancel))
                }
            }
            .build()
    }

    private fun titleResource(purpose: AuthenticationPurpose): Int = when (purpose) {
        AuthenticationPurpose.UNLOCK_VAULT -> R.string.auth_purpose_unlock_vault
        AuthenticationPurpose.AUTOFILL -> R.string.auth_purpose_autofill
        AuthenticationPurpose.BACKUP_EXPORT -> R.string.auth_purpose_backup_export
        AuthenticationPurpose.BACKUP_IMPORT -> R.string.auth_purpose_backup_import
        AuthenticationPurpose.EXPORT_DIAGNOSTICS ->
            R.string.auth_purpose_export_diagnostics

        AuthenticationPurpose.RECOVER_DATABASE -> R.string.auth_purpose_recover_database
        AuthenticationPurpose.RECOVER_AUTH_METHODS -> R.string.auth_purpose_recover_auth_methods
        AuthenticationPurpose.CLEAR_DATABASE -> R.string.auth_purpose_clear_database
        else -> R.string.auth_verify_identity
    }
}

internal object BiometricPromptErrorClassifier {
    fun classify(errorCode: Int): BiometricHostFailure = when (errorCode) {
        BiometricPrompt.ERROR_LOCKOUT,
        BiometricPrompt.ERROR_LOCKOUT_PERMANENT -> BiometricHostFailure.RATE_LIMITED

        BiometricPrompt.ERROR_HW_NOT_PRESENT,
        BiometricPrompt.ERROR_HW_UNAVAILABLE,
        BiometricPrompt.ERROR_NO_BIOMETRICS,
        BiometricPrompt.ERROR_NO_DEVICE_CREDENTIAL,
        BiometricPrompt.ERROR_SECURITY_UPDATE_REQUIRED -> BiometricHostFailure.METHOD_UNAVAILABLE

        BiometricPrompt.ERROR_UNABLE_TO_PROCESS,
        BiometricPrompt.ERROR_NO_SPACE,
        BiometricPrompt.ERROR_VENDOR -> BiometricHostFailure.CRYPTO_OBJECT_INVALID

        else -> BiometricHostFailure.AUTHENTICATION_FAILED
    }
}
