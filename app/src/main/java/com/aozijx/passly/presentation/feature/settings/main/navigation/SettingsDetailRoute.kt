package com.aozijx.passly.presentation.feature.settings.main.navigation

import androidx.compose.runtime.Composable
import com.aozijx.passly.presentation.feature.settings.main.navigation.autofill.AutofillRoute
import com.aozijx.passly.presentation.feature.settings.main.navigation.core.AppearanceRoute
import com.aozijx.passly.presentation.feature.settings.main.navigation.core.InterfaceRoute
import com.aozijx.passly.presentation.feature.settings.main.navigation.core.PrivacyRoute
import com.aozijx.passly.presentation.feature.settings.main.navigation.core.PermissionsRoute
import com.aozijx.passly.presentation.feature.settings.main.navigation.core.RecoveryCodeRoute
import com.aozijx.passly.presentation.feature.settings.main.navigation.core.SecurityRoute
import com.aozijx.passly.presentation.feature.settings.main.navigation.data.BackupRoute
import com.aozijx.passly.presentation.feature.settings.main.navigation.data.DataManagementRoute
import com.aozijx.passly.presentation.feature.settings.main.navigation.general.DiagnosticsRoute
import com.aozijx.passly.presentation.feature.settings.main.navigation.general.GeneralRoute
import com.aozijx.passly.presentation.feature.settings.main.navigation.general.NotificationsRoute
import com.aozijx.passly.presentation.feature.settings.main.navigation.interaction.InteractionRoute
import com.aozijx.passly.presentation.feature.settings.ui.main.SettingsDetailPlaceholder

@Composable
internal fun SettingsDetailRoute(
    route: SettingsDestination?,
    onOpenTrash: () -> Unit,
    onNavigateTo: (SettingsDestination) -> Unit,
    onBack: (() -> Unit)?,
) {
    when (route) {
        null,
        SettingsDestination.Main -> SettingsDetailPlaceholder()
        SettingsDestination.Security -> SecurityRoute(onBack)
        SettingsDestination.Privacy -> PrivacyRoute(onBack)
        SettingsDestination.Permissions -> PermissionsRoute(
            onOpenBackupSettings = {
                onNavigateTo(SettingsDestination.BackupRestore)
            },
            onBack = onBack,
        )
        SettingsDestination.Appearance -> AppearanceRoute(onBack)
        SettingsDestination.Interface -> InterfaceRoute(onBack)
        SettingsDestination.Interaction -> InteractionRoute(onBack)
        SettingsDestination.Autofill -> AutofillRoute(onBack)
        SettingsDestination.DataManagement -> DataManagementRoute(onOpenTrash, onBack)
        SettingsDestination.BackupRestore -> BackupRoute(onBack)
        SettingsDestination.RecoveryCode -> RecoveryCodeRoute(onBack)
        SettingsDestination.General -> GeneralRoute(
            onOpenDiagnostics = { onNavigateTo(SettingsDestination.Diagnostics) },
            onBack = onBack,
        )
        SettingsDestination.Diagnostics -> DiagnosticsRoute(onBack)
        SettingsDestination.Notifications -> NotificationsRoute(onBack)
    }
}
