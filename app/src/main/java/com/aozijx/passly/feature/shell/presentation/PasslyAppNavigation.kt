package com.aozijx.passly.feature.shell.presentation

import androidx.fragment.app.FragmentActivity
import androidx.compose.runtime.Composable
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.compose.rememberNavController
import com.aozijx.passly.core.ui.adaptive.ProvidePasslyAdaptiveLayout
import com.aozijx.passly.feature.settings.presentation.main.navigation.registerSettingsGraph
import com.aozijx.passly.feature.shell.presentation.navigation.PasslyNavHost
import com.aozijx.passly.feature.vault.presentation.navigation.registerVaultGraph
import com.aozijx.passly.feature.vault.presentation.ScannedOtpImportViewModel

@Composable
internal fun PasslyAppNavigation(activity: FragmentActivity) {
    val scannedOtpImportViewModel = hiltViewModel<ScannedOtpImportViewModel>(activity)
    val navController = rememberNavController()

    ProvidePasslyAdaptiveLayout {
        PasslyNavHost(
            navController = navController,
        ) { context, sharedTransitionScope ->
            registerVaultGraph(
                context = context,
                navController = navController,
                sharedTransitionScope = sharedTransitionScope,
                scannedOtpImportViewModel = scannedOtpImportViewModel,
            )
            registerSettingsGraph(context)
        }
    }
}
