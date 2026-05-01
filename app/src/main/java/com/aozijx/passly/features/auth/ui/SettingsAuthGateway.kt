package com.aozijx.passly.features.auth.ui

import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.flow.StateFlow

/**
 * SettingsScreen 使用的最小认证能力边界。
 */
interface SettingsAuthGateway {
    val isAppPasswordEnabled: StateFlow<Boolean>

    fun setAppPassword(password: CharArray, onResult: (Result<Unit>) -> Unit)

    fun changeAppPassword(
        oldPassword: CharArray, newPassword: CharArray, onResult: (Result<Unit>) -> Unit
    )

    fun disableAppPassword(password: CharArray, onResult: (Result<Unit>) -> Unit)

    fun verifyIdentity(
        activity: FragmentActivity,
        title: String,
        subtitle: String,
        onResult: (Result<Unit>) -> Unit
    )
}