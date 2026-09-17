package com.aozijx.passly.presentation.feature.settings.main.navigation

import android.content.Context
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import com.aozijx.passly.presentation.feature.settings.backup.DataManagementSettingsViewModel
import com.aozijx.passly.presentation.feature.settings.main.interaction.InteractionSettingsViewModel
import com.aozijx.passly.presentation.feature.settings.main.navigation.autofill.AutofillRoute
import com.aozijx.passly.presentation.feature.settings.main.navigation.core.RecoveryCodeRoute
import com.aozijx.passly.presentation.feature.settings.main.navigation.data.BackupRoute
import com.aozijx.passly.presentation.feature.settings.main.navigation.data.DataManagementRoute
import com.aozijx.passly.presentation.feature.settings.main.navigation.general.GeneralRoute
import com.aozijx.passly.presentation.feature.settings.main.navigation.general.NotificationsRoute
import com.aozijx.passly.presentation.feature.settings.main.navigation.interaction.InteractionRoute
import com.aozijx.passly.presentation.ui.settings.main.SettingsOverlayState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DataSettingsRoute(
    route: SettingsRoute,
    context: Context,
    localState: SettingsOverlayState,
    interactionViewModel: InteractionSettingsViewModel,
    dataViewModel: DataManagementSettingsViewModel,
    onOpenTrash: () -> Unit,
    onBack: (() -> Unit)?,
) {
    val content: @Composable () -> Unit = when (route) {
        SettingsRoute.Interaction -> ({ InteractionRoute(localState, interactionViewModel, onBack) })
        SettingsRoute.Autofill -> ({ AutofillRoute(onBack) })
        SettingsRoute.DataManagement -> ({ DataManagementRoute(localState, onOpenTrash, onBack) })
        SettingsRoute.BackupRestore -> ({ BackupRoute(context, localState, dataViewModel, onBack) })
        SettingsRoute.RecoveryCode -> ({ RecoveryCodeRoute(context, localState, onBack) })
        SettingsRoute.General -> ({ GeneralRoute(onBack) })
        SettingsRoute.Notifications -> ({ NotificationsRoute(onBack) })
        else -> error("Unsupported data settings route: ${route.route}")
    }
    content()
}
