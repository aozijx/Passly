package com.aozijx.passly.feature.auth

import com.aozijx.passly.feature.auth.biometric.BiometricPromptLauncher
import com.aozijx.passly.core.error.AppResult
import kotlinx.coroutines.flow.StateFlow

interface VerificationGateway {
    val isAuthorized: StateFlow<Boolean>
    val isAppPasswordEnabled: StateFlow<Boolean>

    fun verifyWithBiometric(
        launcher: BiometricPromptLauncher,
        title: String,
        subtitle: String,
        forceReauth: Boolean = false,
        onResult: (AppResult<Unit>) -> Unit
    )

    fun verifyWithAppPassword(
        password: CharArray,
        onResult: (AppResult<Unit>) -> Unit
    )

    fun setAppPassword(password: CharArray, onResult: (AppResult<Unit>) -> Unit)

    fun bootstrapAppPassword(password: CharArray, onResult: (AppResult<Unit>) -> Unit)

    fun changeAppPassword(
        oldPassword: CharArray,
        newPassword: CharArray,
        onResult: (AppResult<Unit>) -> Unit
    )

    fun disableAppPassword(password: CharArray, onResult: (AppResult<Unit>) -> Unit)

    fun rekeyWithInvalidationPolicy(
        launcher: BiometricPromptLauncher,
        invalidateOnBiometricChange: Boolean,
        onResult: (AppResult<Unit>) -> Unit
    )

    suspend fun verifyWithBiometricSuspended(
        launcher: BiometricPromptLauncher,
        title: String,
        subtitle: String
    ): AppResult<Unit>
}
