package com.aozijx.passly.presentation.feature.vault.detail

import com.aozijx.passly.app.security.SensitiveAccessLevel
import com.aozijx.passly.domain.access.model.SensitiveAccessAction

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
