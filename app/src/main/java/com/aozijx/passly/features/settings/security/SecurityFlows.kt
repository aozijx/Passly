package com.aozijx.passly.features.settings.security

import android.content.Context
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import com.aozijx.passly.core.error.AppResult
import com.aozijx.passly.features.common.toUiMessage

internal fun handleInvalidateKeyToggle(
    context: Context,
    activity: FragmentActivity?,
    enabled: Boolean,
    switchPolicy: (FragmentActivity, Boolean, (AppResult<Unit>) -> Unit) -> Unit
) {
    if (activity == null) {
        Toast.makeText(context, "无法进行操作", Toast.LENGTH_SHORT).show()
        return
    }
    switchPolicy(activity, enabled) { result ->
        result.onSuccess {
            Toast.makeText(activity, "安全策略已更新", Toast.LENGTH_SHORT).show()
        }.onFailure { error ->
            Toast.makeText(activity, error.toUiMessage(), Toast.LENGTH_SHORT).show()
        }
    }
}