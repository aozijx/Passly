package com.aozijx.passly.presentation.feature.shell.navigation

import com.aozijx.passly.app.security.SensitiveAccessLevel
import com.aozijx.passly.domain.access.model.SensitiveAccessAction

internal class ShellNavigationContext(
    val navigateBack: () -> Unit,
    val navigateToRoute: (String) -> Unit,
    val navigateToSingleTopRoute: (String) -> Unit,
    val requestAuthentication: (onSuccess: () -> Unit) -> Unit,
    val requestReauthentication: (onSuccess: () -> Unit) -> Unit,
    val requestSensitiveAccess: (
        action: SensitiveAccessAction,
        accessLevel: SensitiveAccessLevel,
        onSuccess: () -> Unit,
    ) -> Unit,
    val onUserInteraction: () -> Unit,
)
