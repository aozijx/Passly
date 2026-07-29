package com.aozijx.passly.feature.detail

import com.aozijx.passly.domain.authentication.SensitiveAccessLevel
import com.aozijx.passly.domain.authentication.SensitiveAccessAction

fun interface DetailAuthenticate {
    fun authenticate(
        action: SensitiveAccessAction,
        accessLevel: SensitiveAccessLevel,
        onSuccess: () -> Unit
    )

    fun reveal(
        accessLevel: SensitiveAccessLevel = SensitiveAccessLevel.STANDARD,
        onSuccess: () -> Unit
    ) {
        authenticate(SensitiveAccessAction.REVEAL, accessLevel, onSuccess)
    }

    fun copy(onSuccess: () -> Unit) {
        authenticate(
            SensitiveAccessAction.COPY,
            SensitiveAccessLevel.STANDARD,
            onSuccess
        )
    }
}
