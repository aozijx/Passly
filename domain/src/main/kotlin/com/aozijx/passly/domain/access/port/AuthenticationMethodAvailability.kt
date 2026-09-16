package com.aozijx.passly.domain.access.port

import com.aozijx.passly.domain.access.model.AuthenticationMethods
import kotlinx.coroutines.flow.StateFlow

/** Read-only availability of authentication methods for presentation and policy consumers. */
interface AuthenticationMethodAvailability {
    val methods: StateFlow<AuthenticationMethods>
}