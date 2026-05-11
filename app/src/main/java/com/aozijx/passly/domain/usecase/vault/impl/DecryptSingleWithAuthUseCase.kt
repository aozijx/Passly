package com.aozijx.passly.domain.usecase.vault.impl

import androidx.fragment.app.FragmentActivity

typealias AuthenticateAction =
            (FragmentActivity, String, String, ((String) -> Unit)?, () -> Unit) -> Unit

/**
 * 认证后解密单字段：当前仅做认证门控，实际解密由上游传入的数据策略决定。
 */
class DecryptSingleWithAuthUseCase {
    operator fun invoke(
        activity: FragmentActivity,
        encryptedData: String,
        promptTitle: String,
        promptSubtitle: String,
        authenticate: AuthenticateAction,
        onResult: (String?) -> Unit
    ) {
        if (encryptedData.isEmpty()) {
            onResult("")
            return
        }

        authenticate(activity, promptTitle, promptSubtitle, null) {
            onResult(encryptedData)
        }
    }
}