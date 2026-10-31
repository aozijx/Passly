package com.aozijx.passly.feature.vault.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aozijx.passly.domain.access.model.AuthenticationState
import com.aozijx.passly.domain.access.port.SecureSessionAccessState
import com.aozijx.passly.domain.entry.model.otp.OtpConfig
import com.aozijx.passly.feature.vault.entry.ImportScannedOtpUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.async
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.sync.Mutex

/** Owns a confirmed OTP import beyond the scanner and Vault destination lifetimes. */
@HiltViewModel
class ScannedOtpImportViewModel @Inject constructor(
    private val importScannedOtpUseCase: ImportScannedOtpUseCase,
    private val access: SecureSessionAccessState,
) : ViewModel() {
    private val importMutex = Mutex()
    private var operationScopeOverride: CoroutineScope? = null

    internal constructor(
        importScannedOtpUseCase: ImportScannedOtpUseCase,
        access: SecureSessionAccessState,
        operationScope: CoroutineScope,
    ) : this(importScannedOtpUseCase, access) {
        operationScopeOverride = operationScope
    }

    suspend fun importScannedOtp(config: OtpConfig): Boolean {
        if (!access.hasFullSecureSessionAccess()) return false
        val confirmedSession = access.authenticationState.value
        if (confirmedSession !is AuthenticationState.Authenticated || !importMutex.tryLock()) {
            return false
        }
        return (operationScopeOverride ?: viewModelScope).async {
            try {
                if (access.authenticationState.value != confirmedSession ||
                    !access.hasFullSecureSessionAccess()
                ) {
                    false
                } else {
                    importScannedOtpUseCase(config)
                }
            } finally {
                importMutex.unlock()
            }
        }.await()
    }
}
