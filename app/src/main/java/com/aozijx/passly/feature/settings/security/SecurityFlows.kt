package com.aozijx.passly.feature.settings.security

import android.content.Context
import com.aozijx.passly.core.message.AppMessageCenter

internal fun handleInvalidateKeyToggle(
    context: Context,
    enabled: Boolean,
    switchPolicy: (Boolean, (Boolean) -> Unit) -> Unit
) {
    switchPolicy(enabled) { success ->
        if (success) AppMessageCenter.publish("安全策略已更新")
    }
}
