package com.aozijx.passly.feature.shell.presentation.navigation

internal class ShellNavigationContext(
    val navigateBack: () -> Unit,
    val navigateToRoute: (String) -> Unit,
    val navigateToSingleTopRoute: (String) -> Unit,
)