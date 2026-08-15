package com.aozijx.passly.presentation.settings.general

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Info
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.aozijx.passly.R
import com.aozijx.passly.core.ui.components.group.RoundedGroup
import com.aozijx.passly.core.ui.components.group.navigationSettingsGroupItem
import com.aozijx.passly.core.ui.components.settings.SettingsSectionTitle

@Composable
fun CacheSettingsSection(
    cacheSize: String?,
    isLoading: Boolean,
    onClearCache: () -> Unit
) {
    SettingsSectionTitle(text = stringResource(R.string.settings_general_cache_section))
    RoundedGroup(
        items = listOf(
            navigationSettingsGroupItem(
                key = "general.clear_cache",
                icon = Icons.Default.DeleteSweep,
                title = stringResource(R.string.settings_general_clear_cache),
                value = cacheSize,
                isLoading = isLoading,
                onClick = onClearCache
            )
        )
    )
}

@Composable
fun AboutSettingsSection(
    appVersion: String,
    onAppDetailsClick: () -> Unit,
    onAboutClick: () -> Unit
) {
    SettingsSectionTitle(text = stringResource(R.string.settings_general_about_section))
    RoundedGroup(
        items = listOf(
            navigationSettingsGroupItem(
                key = "general.about",
                icon = Icons.Default.Info,
                title = stringResource(
                    R.string.settings_general_about_app,
                    stringResource(R.string.app_name)
                ),
                value = appVersion,
                onClick = onAppDetailsClick
            ),
            navigationSettingsGroupItem(
                key = "general.terms",
                iconPlaceholder = true,
                title = stringResource(R.string.settings_general_terms),
                onClick = onAboutClick
            ),
            navigationSettingsGroupItem(
                key = "general.privacy_policy",
                iconPlaceholder = true,
                title = stringResource(R.string.settings_general_privacy_policy),
                onClick = onAboutClick
            ),
            navigationSettingsGroupItem(
                key = "general.open_source",
                iconPlaceholder = true,
                title = stringResource(R.string.settings_general_open_source_licenses),
                onClick = onAboutClick
            )
        )
    )
}
