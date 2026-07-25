package com.aozijx.passly.security.authentication

import com.aozijx.passly.domain.authentication.AuthenticationFailure
import com.aozijx.passly.domain.authentication.AuthenticationMethod

internal sealed interface MethodExecutionResult {
    data class Success(val method: AuthenticationMethod) : MethodExecutionResult
    data class Cancelled(val byUser: Boolean) : MethodExecutionResult
    data class Failure(val failure: AuthenticationFailure) : MethodExecutionResult
}
