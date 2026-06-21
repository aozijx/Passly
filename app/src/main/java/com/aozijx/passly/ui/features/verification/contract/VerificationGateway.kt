package com.aozijx.passly.ui.features.verification.contract

import androidx.fragment.app.FragmentActivity
import com.aozijx.passly.core.error.AppResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface VerificationGateway {
    val isAuthorized: StateFlow<Boolean>
    val isAppPasswordEnabled: StateFlow<Boolean>
    val isDeviceCredentialFallbackEnabled: Flow<Boolean>

    fun verifyWithBiometric(
        activity: FragmentActivity,
        title: String,
        subtitle: String,
        forceReauth: Boolean = false,
        onResult: (AppResult<Unit>) -> Unit
    )

    fun verifyWithDeviceCredential(
        activity: FragmentActivity,
        title: String,
        subtitle: String,
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
}