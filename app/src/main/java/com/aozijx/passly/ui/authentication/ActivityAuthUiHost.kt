package com.aozijx.passly.ui.authentication

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import com.aozijx.passly.domain.authentication.AuthenticationMethod
import com.aozijx.passly.domain.authentication.AuthenticationPurpose
import com.aozijx.passly.security.authentication.SecretChars
import com.aozijx.passly.security.authentication.host.AuthHostSnapshot
import com.aozijx.passly.security.authentication.host.AuthUiHost
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
        return suspendCancellableCoroutine { continuation ->
            val prompt = BiometricPrompt(
                activity,
                activity.mainExecutor,
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        activePrompt.set(null)
                        if (continuation.isActive) continuation.resume(BiometricHostResult.Success(result))
                    }

                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                        activePrompt.set(null)
                        if (!continuation.isActive) return
                        val result = when (errorCode) {
                            BiometricPrompt.ERROR_NEGATIVE_BUTTON,
                            BiometricPrompt.ERROR_USER_CANCELED -> BiometricHostResult.Cancelled(true)
                            BiometricPrompt.ERROR_CANCELED -> BiometricHostResult.Cancelled(false)
                            else -> BiometricHostResult.Error(errorCode)
                        }
                        continuation.resume(result)
                    }
                }
            )
            activePrompt.set(prompt)
            continuation.invokeOnCancellation {
                activePrompt.getAndSet(null)?.cancelAuthentication()
            }
            if (!snapshot().usable) {
                activePrompt.set(null)
                continuation.resume(BiometricHostResult.HostUnavailable)
                return@suspendCancellableCoroutine
            }
            runCatching {
                val promptInfo = BiometricPromptSpecFactory.create(spec)
                if (cryptoObject == null) prompt.authenticate(promptInfo)
                else prompt.authenticate(promptInfo, cryptoObject)
            }.onFailure {
                activePrompt.set(null)
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
    fun create(spec: BiometricPromptSpec): BiometricPrompt.PromptInfo {
        val authenticators = if (spec.allowDeviceCredential) {
            BiometricManager.Authenticators.BIOMETRIC_STRONG or
                BiometricManager.Authenticators.DEVICE_CREDENTIAL
        } else {
            BiometricManager.Authenticators.BIOMETRIC_STRONG
        }
        return BiometricPrompt.PromptInfo.Builder()
            .setTitle(titleFor(spec.purpose))
            .setSubtitle("请验证身份以继续")
            .setAllowedAuthenticators(authenticators)
            .apply {
                if (!spec.allowDeviceCredential) setNegativeButtonText("取消")
            }
            .build()
    }

    private fun titleFor(purpose: AuthenticationPurpose): String = when (purpose) {
        AuthenticationPurpose.UNLOCK_VAULT -> "解锁保险库"
        AuthenticationPurpose.AUTOFILL -> "验证后自动填充"
        AuthenticationPurpose.BACKUP_EXPORT -> "验证后导出备份"
        AuthenticationPurpose.BACKUP_IMPORT -> "验证后导入备份"
        AuthenticationPurpose.EXPORT_DIAGNOSTICS -> "验证后导出诊断"
        else -> "验证身份"
    }
}
