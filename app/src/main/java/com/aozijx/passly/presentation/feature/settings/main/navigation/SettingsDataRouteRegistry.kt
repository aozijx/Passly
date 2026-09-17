package com.aozijx.passly.presentation.feature.settings.main.navigation

import android.content.Context
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import com.aozijx.passly.presentation.feature.settings.main.navigation.autofill.AutofillRoute
import com.aozijx.passly.presentation.feature.settings.main.navigation.core.RecoveryCodeRoute
import com.aozijx.passly.presentation.feature.settings.main.navigation.data.BackupRoute
import com.aozijx.passly.presentation.feature.settings.main.navigation.data.DataManagementRoute
import com.aozijx.passly.presentation.feature.settings.main.navigation.general.GeneralRoute
import com.aozijx.passly.presentation.feature.settings.main.navigation.general.NotificationsRoute
import com.aozijx.passly.presentation.feature.settings.main.navigation.interaction.InteractionRoute

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DataSettingsRoute(
    route: SettingsDestination,
    context: Context,
    onOpenTrash: () -> Unit,
    onBack: (() -> Unit)?,
) {
    val content: @Composable () -> Unit = when (route) {
        SettingsDestination.Interaction -> ({ InteractionRoute(onBack) })
        SettingsDestination.Autofill -> ({ AutofillRoute(onBack) })
        SettingsDestination.DataManagement -> ({ DataManagementRoute(onOpenTrash, onBack) })
        SettingsDestination.BackupRestore -> ({ BackupRoute(context, onBack) })
        SettingsDestination.RecoveryCode -> ({ RecoveryCodeRoute(onBack) })
        SettingsDestination.General -> ({ GeneralRoute(onBack) })
        SettingsDestination.Notifications -> ({ NotificationsRoute(onBack) })
        else -> error("Unsupported data settings route: ${route.route}")
    }
    content()
}
