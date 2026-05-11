package com.aozijx.passly.features.settings.components.sections

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aozijx.passly.R
import com.aozijx.passly.features.settings.components.common.ClickableSettingItem
import com.aozijx.passly.features.settings.components.common.SettingsCard
import com.aozijx.passly.features.settings.components.common.SettingsGroupTitle
import com.aozijx.passly.features.settings.components.common.SwitchSettingItem
import com.aozijx.passly.features.settings.components.common.formatLockTimeoutText

@Composable
fun SecurityPrivacySettingsSection(
    lockTimeout: Long,
    isAppPasswordEnabled: Boolean,
    isPasswordPreferredAuthFirst: Boolean,
    isDeviceCredentialFallbackEnabled: Boolean,
    isInvalidateKeyOnBioChange: Boolean,
    isSecureContentEnabled: Boolean,
    isFlipToLockEnabled: Boolean,
    isFlipExitAndClearStackEnabled: Boolean,
    onLockTimeoutClick: () -> Unit,
    onAppPasswordClick: () -> Unit,
    onPasswordPreferredAuthFirstChange: (Boolean) -> Unit,
    onDeviceCredentialFallbackToggleRequested: (Boolean) -> Unit,
    onInvalidateKeyOnBioChangeToggle: (Boolean) -> Unit,
    onSecureContentEnabledChange: (Boolean) -> Unit,
    onFlipToLockEnabledChange: (Boolean) -> Unit,
    onFlipExitAndClearStackEnabledChange: (Boolean) -> Unit
) {
    SettingsGroupTitle(text = stringResource(R.string.settings_security_privacy_title))
    SettingsCard {
        ClickableSettingItem(
            icon = Icons.Default.Timer,
            title = stringResource(R.string.settings_auto_lock_title),
            value = formatLockTimeoutText(lockTimeout),
            onClick = onLockTimeoutClick
        )
        HorizontalDivider(Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp)
        ClickableSettingItem(
            icon = Icons.Default.Lock,
            title = stringResource(R.string.settings_app_password_title),
            value = if (isAppPasswordEnabled) {
                stringResource(R.string.settings_app_password_status_enabled)
            } else {
                stringResource(R.string.settings_app_password_status_disabled)
            },
            longValue = stringResource(R.string.settings_app_password_description),
            onClick = onAppPasswordClick
        )
        HorizontalDivider(Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp)
        SwitchSettingItem(
            icon = Icons.Default.Lock,
            title = stringResource(R.string.settings_password_priority_title),
            subtitle = stringResource(R.string.settings_password_priority_subtitle),
            checked = isPasswordPreferredAuthFirst,
            onCheckedChange = onPasswordPreferredAuthFirstChange
        )
        HorizontalDivider(Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp)
        SwitchSettingItem(
            icon = Icons.Default.Fingerprint,
            title = stringResource(R.string.settings_device_credential_fallback_title),
            subtitle = stringResource(R.string.settings_device_credential_fallback_subtitle),
            checked = isDeviceCredentialFallbackEnabled,
            onCheckedChange = onDeviceCredentialFallbackToggleRequested
        )
        HorizontalDivider(Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp)
        SwitchSettingItem(
            icon = Icons.Default.Fingerprint,
            title = stringResource(R.string.settings_key_invalidation_title),
            subtitle = if (isInvalidateKeyOnBioChange)
                stringResource(R.string.settings_key_invalidation_subtitle_on)
            else
                stringResource(R.string.settings_key_invalidation_subtitle_off),
            checked = isInvalidateKeyOnBioChange,
            onCheckedChange = onInvalidateKeyOnBioChangeToggle
        )
        HorizontalDivider(Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp)
        SwitchSettingItem(
            icon = Icons.Default.Security,
            title = stringResource(R.string.settings_secure_content_title),
            subtitle = stringResource(R.string.settings_secure_content_subtitle),
            checked = isSecureContentEnabled,
            onCheckedChange = onSecureContentEnabledChange
        )
        HorizontalDivider(Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp)
        SwitchSettingItem(
            icon = Icons.Default.Flip,
            title = stringResource(R.string.settings_flip_lock_title),
            subtitle = stringResource(R.string.settings_flip_lock_subtitle),
            checked = isFlipToLockEnabled,
            onCheckedChange = onFlipToLockEnabledChange
        )

        AnimatedVisibility(
            visible = isFlipToLockEnabled,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Column {
                HorizontalDivider(
                    Modifier.padding(start = 56.dp, end = 16.dp), thickness = 0.5.dp
                )
                SwitchSettingItem(
                    title = stringResource(R.string.settings_flip_exit_title),
                    subtitle = stringResource(R.string.settings_flip_exit_subtitle),
                    checked = isFlipExitAndClearStackEnabled,
                    onCheckedChange = onFlipExitAndClearStackEnabledChange
                )
            }
        }
    }
}