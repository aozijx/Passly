package com.aozijx.passly.features.auth.ui

import androidx.fragment.app.FragmentActivity
import com.aozijx.passly.core.error.AppResult
import kotlinx.coroutines.flow.StateFlow

interface SettingsAuthGateway {
    val isAppPasswordEnabled: StateFlow<Boolean>

    fun setAppPassword(password: CharArray, onResult: (AppResult<Unit>) -> Unit)

    fun changeAppPassword(
        oldPassword: CharArray, newPassword: CharArray, onResult: (AppResult<Unit>) -> Unit
    )

    fun disableAppPassword(password: CharArray, onResult: (AppResult<Unit>) -> Unit)

    fun verifyIdentity(
        activity: FragmentActivity,
        title: String,
        subtitle: String,
        onResult: (AppResult<Unit>) -> Unit
    )
}