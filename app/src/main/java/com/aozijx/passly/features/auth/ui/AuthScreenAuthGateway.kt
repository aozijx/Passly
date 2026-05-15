package com.aozijx.passly.features.auth.ui

import androidx.fragment.app.FragmentActivity
import com.aozijx.passly.core.error.AppResult
import kotlinx.coroutines.flow.StateFlow

interface AuthScreenAuthGateway {
    val isAppPasswordEnabled: StateFlow<Boolean>

    fun authenticate(
        activity: FragmentActivity,
        title: String,
        subtitle: String,
        onSuccess: () -> Unit,
        onError: ((String) -> Unit)?
    )

    fun authenticateWithAppPassword(
        password: CharArray,
        onSuccess: () -> Unit,
        onError: ((String) -> Unit)?
    )

    fun setAppPassword(password: CharArray, onResult: (AppResult<Unit>) -> Unit)

    fun bootstrapAppPassword(password: CharArray, onResult: (AppResult<Unit>) -> Unit)
}