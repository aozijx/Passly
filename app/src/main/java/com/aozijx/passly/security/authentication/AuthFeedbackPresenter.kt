package com.aozijx.passly.security.authentication

import com.aozijx.passly.core.message.AppMessage
import com.aozijx.passly.core.message.AppMessagePublisher
import com.aozijx.passly.domain.authentication.AuthenticationFailure
import com.aozijx.passly.domain.authentication.AuthenticationFailureCode
import com.aozijx.passly.domain.authentication.AuthenticationResult
import java.util.LinkedHashSet
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthFeedbackPresenter @Inject constructor(
    private val messages: AppMessagePublisher
) {
    private val published = LinkedHashSet<String>()

    fun present(result: AuthenticationResult, correlationId: String) {
        val text = when (result) {
            is AuthenticationResult.Success -> return
            is AuthenticationResult.Cancelled -> if (result.byUser) return else "认证已由系统取消"
            is AuthenticationResult.Failure -> result.failure.userMessage()
        }
        synchronized(published) {
            if (!published.add(correlationId)) return
            if (published.size > MAX_TRACKED) published.remove(published.first())
        }
        messages.publish(AppMessage(text = text))
    }

    private fun AuthenticationFailure.userMessage(): String = when (authCode) {
        AuthenticationFailureCode.BUSY -> "已有认证正在进行"
        AuthenticationFailureCode.HOST_UNAVAILABLE -> "当前页面无法进行认证"
        AuthenticationFailureCode.METHOD_UNAVAILABLE -> "所选认证方式不可用"
        AuthenticationFailureCode.KEY_MISSING -> "生物识别密钥不存在，请重新设置"
        AuthenticationFailureCode.KEY_INVALIDATED -> "生物识别已变化，请重新设置"
        AuthenticationFailureCode.CRYPTO_OBJECT_INVALID -> "认证加密状态无效，请重试"
        AuthenticationFailureCode.CREDENTIAL_INCORRECT -> "凭据不正确"
        AuthenticationFailureCode.ENVELOPE_CORRUPTED -> "认证数据已损坏"
        AuthenticationFailureCode.LAST_METHOD_REQUIRED -> "至少保留一种认证方式"
        AuthenticationFailureCode.RATE_LIMITED -> "尝试次数过多，请稍后再试"
        AuthenticationFailureCode.SESSION_TRANSITION_FAILED -> "保险库状态切换失败"
    }

    private companion object {
        const val MAX_TRACKED = 128
    }
}
