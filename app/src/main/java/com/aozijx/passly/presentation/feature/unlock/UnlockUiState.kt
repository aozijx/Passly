package com.aozijx.passly.presentation.feature.unlock

import com.aozijx.passly.domain.access.model.AuthenticationFailure
import com.aozijx.passly.domain.access.model.AuthenticationMethod
import com.aozijx.passly.domain.sensitive.EmptySensitiveValue
import com.aozijx.passly.domain.sensitive.SensitiveValue

data class UnlockVerificationFailure(
    val method: AuthenticationMethod,
    val failure: AuthenticationFailure
)

data class UnlockUiState(
    val appPassword: SensitiveValue = EmptySensitiveValue,
    val recoveryCode: SensitiveValue = EmptySensitiveValue,
    val recoveryUnlockVisible: Boolean = false,
    val expandedMethod: AuthenticationMethod? = null,
    val activeMethod: AuthenticationMethod? = null,
    val verificationFailure: UnlockVerificationFailure? = null
)
