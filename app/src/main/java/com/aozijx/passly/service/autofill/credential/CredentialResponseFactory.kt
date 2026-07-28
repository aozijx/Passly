package com.aozijx.passly.service.autofill.credential

import android.content.Intent
import androidx.credentials.GetCredentialResponse
import androidx.credentials.PasswordCredential
import androidx.credentials.provider.PendingIntentHandler

/**
 * Credential 响应工厂：负责构建 GetCredentialResponse 返回给系统。
 */
internal object CredentialResponseFactory {

    fun buildPasswordResponse(username: String, password: String): Intent {
        val result = Intent()
        PendingIntentHandler.setGetCredentialResponse(
            result,
            GetCredentialResponse(PasswordCredential(username, password)),
        )
        return result
    }
}
