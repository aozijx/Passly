package com.aozijx.passly.presentation.feature.shell.navigation

internal class ShellNavigationContext(
    val navigateBack: () -> Unit,
    val navigateToRoute: (String) -> Unit,
    val navigateToSingleTopRoute: (String) -> Unit,
    val onUserInteraction: () -> Unit,
)