package com.aozijx.passly.app.navigation

internal class ShellNavigationContext(
    val navigateBack: () -> Unit,
    val navigateToRoute: (String) -> Unit,
    val navigateToSingleTopRoute: (String) -> Unit,
)