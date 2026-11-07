package com.aozijx.passly.feature.autofill.core

import com.aozijx.passly.domain.access.model.AuthenticationState
import com.aozijx.passly.domain.access.port.SecureSessionAccessState

/** Returns the live authorized session generation used to bind one-shot web consent. */
internal fun SecureSessionAccessState.autofillSessionGeneration(): Long? {
    if (!hasAutofillSessionAccess()) return null
    return when (val state = authenticationState.value) {
        is AuthenticationState.Authenticated -> state.authenticatedAtMs
        is AuthenticationState.AutofillAuthenticated -> state.authenticatedAtMs
        else -> null
    }
}
