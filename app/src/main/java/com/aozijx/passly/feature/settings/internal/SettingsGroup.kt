package com.aozijx.passly.feature.settings.internal

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.SpaceDashboard
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.ui.graphics.vector.ImageVector
import com.aozijx.passly.R
import com.aozijx.passly.feature.settings.navigation.SettingsRoute

internal enum class SettingsGroup(
    @param:StringRes val sectionTitleRes: Int,
    @param:StringRes val titleRes: Int,
    @param:StringRes val subtitleRes: Int,
    val icon: ImageVector,
    val route: SettingsRoute
) {
    APPEARANCE(
        sectionTitleRes = R.string.settings_section_appearance,
        icon = Icons.Default.Palette,
        titleRes = R.string.settings_page_appearance,
        subtitleRes = R.string.settings_page_appearance_summary,
        route = SettingsRoute.Appearance
    ),
    INTERFACE(
        sectionTitleRes = R.string.settings_section_appearance,
        icon = Icons.Default.SpaceDashboard,
        titleRes = R.string.settings_page_interface,
        subtitleRes = R.string.settings_page_interface_summary,
        route = SettingsRoute.Interface
    ),
    SECURITY(
        sectionTitleRes = R.string.settings_section_security,
        icon = Icons.Default.Lock,
        titleRes = R.string.settings_page_security,
        subtitleRes = R.string.settings_page_security_summary,
        route = SettingsRoute.Security
    ),
    PRIVACY(
        sectionTitleRes = R.string.settings_section_security,
        icon = Icons.Default.Visibility,
        titleRes = R.string.settings_page_privacy,
        subtitleRes = R.string.settings_page_privacy_summary,
        route = SettingsRoute.Privacy
    ),
    INTERACTION(
        sectionTitleRes = R.string.settings_section_features,
        icon = Icons.Default.TouchApp,
        titleRes = R.string.settings_page_interaction,
        subtitleRes = R.string.settings_page_interaction_summary,
        route = SettingsRoute.Interaction
    ),
    DATA_MANAGEMENT(
        sectionTitleRes = R.string.settings_section_data,
        icon = Icons.Default.Storage,
        titleRes = R.string.settings_page_data,
        subtitleRes = R.string.settings_page_data_summary,
        route = SettingsRoute.DataManagement
    ),
    BACKUP_RESTORE(
        sectionTitleRes = R.string.settings_section_data,
        icon = Icons.Default.Restore,
        titleRes = R.string.settings_page_backup_restore,
        subtitleRes = R.string.settings_page_backup_restore_summary,
        route = SettingsRoute.BackupRestore
    ),
    RECOVERY_CODE(
        sectionTitleRes = R.string.settings_section_data,
        icon = Icons.Default.Key,
        titleRes = R.string.settings_page_recovery_code,
        subtitleRes = R.string.settings_page_recovery_code_summary,
        route = SettingsRoute.RecoveryCode
    ),
    NOTIFICATIONS(
        sectionTitleRes = R.string.settings_section_other,
        icon = Icons.Default.Notifications,
        titleRes = R.string.settings_page_notifications,
        subtitleRes = R.string.settings_page_notifications_summary,
        route = SettingsRoute.Notifications
    ),
    GENERAL(
        sectionTitleRes = R.string.settings_section_other,
        icon = Icons.Default.Info,
        titleRes = R.string.settings_page_general,
        subtitleRes = R.string.settings_page_general_summary,
        route = SettingsRoute.General
    )
}
