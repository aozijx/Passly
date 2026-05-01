package com.aozijx.passly.features.auth.ui

import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.flow.StateFlow

/**
 * AuthScreen 使用的最小认证能力边界，避免 UI 直接依赖具体协调器实现。
 */
interface AuthScreenAuthGateway {
    val isAppPasswordEnabled: StateFlow<Boolean>

    fun authenticate(
        activity: FragmentActivity,
        title: String,
        subtitle: String,
        onSuccess: () -> Unit = {},
        onError: ((String) -> Unit)? = null
    )

    fun authenticateWithAppPassword(
        password: CharArray,
        onSuccess: () -> Unit = {},
        onError: ((String) -> Unit)? = null
    )

    fun bootstrapAppPassword(password: CharArray, onResult: (Result<Unit>) -> Unit)
}