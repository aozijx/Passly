package com.aozijx.passly.feature.autofill.credential.service

import android.content.Intent
import androidx.credentials.CreatePasswordResponse
import androidx.credentials.GetCredentialResponse
import androidx.credentials.PasswordCredential
import androidx.credentials.exceptions.CreateCredentialException
import androidx.credentials.exceptions.GetCredentialException
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

    fun buildGetException(exception: GetCredentialException): Intent {
        return Intent().also {
            PendingIntentHandler.setGetCredentialException(it, exception)
        }
    }

    fun buildPasswordCreateResponse(): Intent {
        return Intent().also {
            PendingIntentHandler.setCreateCredentialResponse(it, CreatePasswordResponse())
        }
    }

    fun buildCreateException(exception: CreateCredentialException): Intent {
        return Intent().also {
            PendingIntentHandler.setCreateCredentialException(it, exception)
        }
    }
}
