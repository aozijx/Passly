package com.aozijx.passly.presentation

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.fragment.app.FragmentActivity
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.aozijx.passly.core.ui.adaptive.ProvidePasslyAdaptiveLayout
import com.aozijx.passly.feature.settings.presentation.main.navigation.registerSettingsGraph
import com.aozijx.passly.feature.shell.presentation.navigation.PasslyNavHost
import com.aozijx.passly.feature.vault.presentation.ScannedOtpImportViewModel
import com.aozijx.passly.feature.vault.presentation.navigation.registerVaultGraph
import com.aozijx.passly.feature.vault.presentation.trash.TrashRoute

@Composable
internal fun PasslyAppNavigation(activity: FragmentActivity) {
    val scannedOtpImportViewModel = hiltViewModel<ScannedOtpImportViewModel>(activity)
    val navController = rememberNavController()
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    var trashSheetVisible by remember { mutableStateOf(false) }

    ProvidePasslyAdaptiveLayout {
        Box {
            PasslyNavHost(
                navController = navController,
            ) { context, sharedTransitionScope ->
                registerVaultGraph(
                    context = context,
                    navController = navController,
                    sharedTransitionScope = sharedTransitionScope,
                    scannedOtpImportViewModel = scannedOtpImportViewModel,
                )
                registerSettingsGraph(context) { trashSheetVisible = true }
            }

            if (trashSheetVisible && currentBackStackEntry != null) {
                TrashRoute(
                    viewModelStoreOwner = currentBackStackEntry!!,
                    onDismiss = { trashSheetVisible = false },
                )
            }
        }
    }
}
