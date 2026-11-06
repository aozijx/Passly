package com.aozijx.passly.domain.access.port

import com.aozijx.passly.domain.access.model.AuthenticationKdfParameters

interface AuthenticationKdfConfigurationStore {
    suspend fun load(): AuthenticationKdfParameters?

    /** First writer wins; a missing configuration with existing envelopes is an error. */
    suspend fun initializeIfAbsent(parameters: AuthenticationKdfParameters): AuthenticationKdfParameters
}
