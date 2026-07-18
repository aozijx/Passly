package com.aozijx.passly.security.authentication

import com.aozijx.passly.core.message.AppMessage
import com.aozijx.passly.core.message.AppMessagePublisher
import com.aozijx.passly.domain.authentication.AuthenticationFailure
import com.aozijx.passly.domain.authentication.AuthenticationFailureCode
import com.aozijx.passly.domain.authentication.AuthenticationResult
import org.junit.Assert.assertEquals
import org.junit.Test

class AuthFeedbackPresenterTest {
    @Test
    fun userCancellationIsSilentAndFailureIsPublishedOncePerCorrelationId() {
        val messages = mutableListOf<AppMessage>()
        val presenter = AuthFeedbackPresenter(AppMessagePublisher(messages::add))

        presenter.present(AuthenticationResult.Cancelled(byUser = true), "cancelled")
        val failure = AuthenticationResult.Failure(
            AuthenticationFailure(AuthenticationFailureCode.HOST_UNAVAILABLE, "same")
        )
        presenter.present(failure, "same")
        presenter.present(failure, "same")

        assertEquals(1, messages.size)
        assertEquals("当前页面无法进行认证", messages.single().text)
    }
}
