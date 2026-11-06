package com.aozijx.passly.domain.access.port

import com.aozijx.passly.domain.access.model.AuthenticationKdfParameters

interface AuthenticationKdfParameterSource {
    suspend fun selected(): AuthenticationKdfParameters
}
