package com.aozijx.passly.feature.settings.security

import android.content.Context
import android.widget.Toast
import com.aozijx.passly.feature.auth.biometric.BiometricPromptLauncher
import com.aozijx.passly.core.error.AppResult
import com.aozijx.passly.core.error.ui.toUiMessage

internal fun handleInvalidateKeyToggle(
    context: Context,
    launcher: BiometricPromptLauncher?,
    enabled: Boolean,
    switchPolicy: (BiometricPromptLauncher, Boolean, (AppResult<Unit>) -> Unit) -> Unit
) {
    if (launcher == null) {
        Toast.makeText(context, "无法进行操作", Toast.LENGTH_SHORT).show()
        return
    }
    switchPolicy(launcher, enabled) { result ->
        result.onSuccess {
            Toast.makeText(context, "安全策略已更新", Toast.LENGTH_SHORT).show()
        }.onFailure { error ->
            Toast.makeText(context, error.toUiMessage(), Toast.LENGTH_SHORT).show()
        }
    }
}