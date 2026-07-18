package com.aozijx.passly.feature.settings.security

import com.aozijx.passly.core.message.AppMessageCenter

internal fun handleInvalidateKeyToggle(
    enabled: Boolean,
    switchPolicy: (Boolean, (Boolean) -> Unit) -> Unit
) {
    switchPolicy(enabled) { success ->
        if (success) AppMessageCenter.publish("安全策略已更新")
    }
}

internal fun handleBiometricToggle(
    enabled: Boolean,
    setEnabled: (Boolean, (Boolean) -> Unit) -> Unit
) {
    setEnabled(enabled) { success ->
        if (success) {
            AppMessageCenter.publish(if (enabled) "已启用生物识别" else "已停用生物识别")
        }
    }
}
