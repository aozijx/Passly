package com.aozijx.passly.presentation.settings.security.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.Security
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.aozijx.passly.R
import com.aozijx.passly.core.ui.components.group.RoundedGroup
import com.aozijx.passly.core.ui.components.group.switchSettingsGroupItem
import com.aozijx.passly.core.ui.components.settings.SettingsSectionTitle

@Composable
fun SecurityProtectionSettingsSection(
    isSecureContentEnabled: Boolean,
    isFlipToLockEnabled: Boolean,
    isFlipExitAndClearStackEnabled: Boolean,
    onSecureContentEnabledChange: (Boolean) -> Unit,
    onFlipToLockEnabledChange: (Boolean) -> Unit,
    onFlipExitAndClearStackEnabledChange: (Boolean) -> Unit
) {
    SettingsSectionTitle(text = stringResource(R.string.settings_security_protection_section))
    RoundedGroup(
        items = listOf(
            switchSettingsGroupItem(
                key = "privacy.secure_content",
                icon = Icons.Default.Security,
                title = stringResource(R.string.settings_security_secure_content),
                subtitle = stringResource(R.string.settings_security_secure_content_description),
                checked = isSecureContentEnabled,
                onCheckedChange = onSecureContentEnabledChange
            ),
            switchSettingsGroupItem(
                key = "privacy.flip_to_lock",
                icon = Icons.Default.Flip,
                title = stringResource(R.string.settings_security_flip_to_lock),
                subtitle = stringResource(R.string.settings_security_flip_to_lock_description),
                checked = isFlipToLockEnabled,
                onCheckedChange = onFlipToLockEnabledChange
            ),
            switchSettingsGroupItem(
                key = "privacy.flip_exit",
                visible = isFlipToLockEnabled,
                iconPlaceholder = true,
                title = stringResource(R.string.settings_security_flip_exit),
                subtitle = stringResource(R.string.settings_security_flip_exit_description),
                checked = isFlipExitAndClearStackEnabled,
                onCheckedChange = onFlipExitAndClearStackEnabledChange
            )
        )
    )
}
