package com.aozijx.passly.features.verification.contract

import androidx.fragment.app.FragmentActivity
import com.aozijx.passly.core.error.AppResult
import kotlinx.coroutines.flow.StateFlow

interface VerificationGateway {
    val isAuthorized: StateFlow<Boolean>
    val isAppPasswordEnabled: StateFlow<Boolean>

    fun verifyWithBiometric(
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