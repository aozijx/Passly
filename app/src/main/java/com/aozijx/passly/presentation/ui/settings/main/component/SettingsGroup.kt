package com.aozijx.passly.presentation.ui.settings.main.component

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
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.ui.graphics.vector.ImageVector
import com.aozijx.passly.R

internal enum class SettingsGroup(
    @param:StringRes val sectionTitleRes: Int,
    @param:StringRes val titleRes: Int,
    @param:StringRes val subtitleRes: Int,
    val icon: ImageVector,
    val routeKey: String
) {
    APPEARANCE(
        sectionTitleRes = R.string.settings_section_appearance,
        icon = Icons.Default.Palette,
        titleRes = R.string.settings_page_appearance,
        subtitleRes = R.string.settings_page_appearance_summary,
        routeKey = "settings/appearance"
    ),
    INTERFACE(
        sectionTitleRes = R.string.settings_section_appearance,
        icon = Icons.Default.SpaceDashboard,
        titleRes = R.string.settings_page_interface,
        subtitleRes = R.string.settings_page_interface_summary,
        routeKey = "settings/interface"
    ),
    SECURITY(
        sectionTitleRes = R.string.settings_section_security,
        icon = Icons.Default.Lock,
        titleRes = R.string.settings_page_security,
        subtitleRes = R.string.settings_page_security_summary,
        routeKey = "settings/security"
    ),
    PRIVACY(
        sectionTitleRes = R.string.settings_section_security,
        icon = Icons.Default.Visibility,
        titleRes = R.string.settings_page_privacy,
        subtitleRes = R.string.settings_page_privacy_summary,
        routeKey = "settings/privacy"
    ),
    INTERACTION(
        sectionTitleRes = R.string.settings_section_features,
        icon = Icons.Default.TouchApp,
        titleRes = R.string.settings_page_interaction,
        subtitleRes = R.string.settings_page_interaction_summary,
        routeKey = "settings/interaction"
    ),
    AUTOFILL(
        sectionTitleRes = R.string.settings_section_features,
        icon = Icons.Default.VpnKey,
        titleRes = R.string.settings_page_autofill,
        subtitleRes = R.string.settings_page_autofill_summary,
        routeKey = "settings/autofill"
    ),
    DATA_MANAGEMENT(
        sectionTitleRes = R.string.settings_section_data,
        icon = Icons.Default.Storage,
        titleRes = R.string.settings_page_data,
        subtitleRes = R.string.settings_page_data_summary,
        routeKey = "settings/data"
    ),
    BACKUP_RESTORE(
        sectionTitleRes = R.string.settings_section_data,
        icon = Icons.Default.Restore,
        titleRes = R.string.settings_page_backup_restore,
        subtitleRes = R.string.settings_page_backup_restore_summary,
        routeKey = "settings/backup_restore"
    ),
    RECOVERY_CODE(
        sectionTitleRes = R.string.settings_section_data,
        icon = Icons.Default.Key,
        titleRes = R.string.recovery_code_label,
        subtitleRes = R.string.settings_page_recovery_code_summary,
        routeKey = "settings/recovery_code"
    ),
    NOTIFICATIONS(
        sectionTitleRes = R.string.settings_section_other,
        icon = Icons.Default.Notifications,
        titleRes = R.string.settings_page_notifications,
        subtitleRes = R.string.settings_page_notifications_summary,
        routeKey = "settings/notifications"
    ),
    GENERAL(
        sectionTitleRes = R.string.settings_section_other,
        icon = Icons.Default.Info,
        titleRes = R.string.settings_page_general,
        subtitleRes = R.string.settings_page_general_summary,
        routeKey = "settings/general"
    )
}
