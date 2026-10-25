package com.aozijx.passly.feature.settings.presentation.ui.about

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.aozijx.passly.R
import com.aozijx.passly.core.ui.components.settings.SettingsSectionTitle
import com.aozijx.passly.presentation.shared.components.group.SegmentedSettingsGroup
import com.aozijx.passly.presentation.shared.components.group.navigationSettingsGroupItem

@Composable
internal fun AboutSettingsSection(
    appVersion: String,
    onAppDetailsClick: () -> Unit,
    onTermsClick: () -> Unit,
    onPrivacyPolicyClick: () -> Unit,
    onOpenSourceLicensesClick: () -> Unit,
) {
    SettingsSectionTitle(text = stringResource(R.string.settings_about_section))
    SegmentedSettingsGroup(
        items = listOf(
            navigationSettingsGroupItem(
                key = "about.app",
                icon = Icons.Default.Info,
                title = stringResource(R.string.settings_about_app, stringResource(R.string.app_name)),
                value = appVersion,
                onClick = onAppDetailsClick,
            ),
            navigationSettingsGroupItem(
                key = "about.terms",
                title = stringResource(R.string.settings_about_terms),
                onClick = onTermsClick,
            ),
            navigationSettingsGroupItem(
                key = "about.privacy_policy",
                title = stringResource(R.string.settings_about_privacy_policy),
                onClick = onPrivacyPolicyClick,
            ),
            navigationSettingsGroupItem(
                key = "about.open_source",
                title = stringResource(R.string.settings_about_open_source_licenses),
                onClick = onOpenSourceLicensesClick,
            ),
        ),
    )
}
