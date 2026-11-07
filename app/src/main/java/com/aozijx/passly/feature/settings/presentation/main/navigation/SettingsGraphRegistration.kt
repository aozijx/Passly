package com.aozijx.passly.feature.settings.presentation.main.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.aozijx.passly.app.navigation.AppRoute
import com.aozijx.passly.app.navigation.ShellNavigationContext

internal fun NavGraphBuilder.registerSettingsGraph(
    context: ShellNavigationContext,
    onOpenTrash: () -> Unit,
) {
    composable(AppRoute.Settings.route) {
        SettingsRoute(
            onOuterBack = context.navigateBack,
            onOpenTrash = onOpenTrash,
        )
    }
}
