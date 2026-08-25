package com.aozijx.passly.presentation.ui.settings.security

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentPasteOff
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aozijx.passly.R
import com.aozijx.passly.core.ui.components.group.RoundedGroup
import com.aozijx.passly.core.ui.components.group.settingsGroupItem
import com.aozijx.passly.core.ui.components.group.switchSettingsGroupItem
import com.aozijx.passly.core.ui.components.group.dropdownSettingsGroupItem
import com.aozijx.passly.core.ui.components.settings.SettingsSection
import com.aozijx.passly.core.ui.components.settings.SettingsSectionTitle
import com.aozijx.passly.presentation.ui.settings.security.model.PrivacySettingsUiModel

@Composable
internal fun PrivacyDetail(
    state: PrivacySettingsUiModel,
    onSecureContentEnabledChange: (Boolean) -> Unit,
    onFlipToLockEnabledChange: (Boolean) -> Unit,
    onFlipExitAndClearStackEnabledChange: (Boolean) -> Unit,
    onSensitiveCopyReauthenticationChange: (Boolean) -> Unit,
    onClipboardClearEnabledChange: (Boolean) -> Unit,
    onClipboardClearDelayChange: (Int) -> Unit,
    onClearClipboard: () -> Unit,
) {
    var showClipboardDelayMenu by remember { mutableStateOf(false) }
    val delayOptions = state.clipboardClearDelayOptions.map { seconds ->
        seconds to stringResource(R.string.settings_privacy_clipboard_delay_seconds, seconds)
    }
    SettingsSection {
        Spacer(modifier = Modifier.height(8.dp))
        SecurityProtectionSettingsSection(
            isSecureContentEnabled = state.isSecureContentEnabled,
            isFlipToLockEnabled = state.isFlipToLockEnabled,
            isFlipExitAndClearStackEnabled = state.isFlipExitAndClearStackEnabled,
            onSecureContentEnabledChange = onSecureContentEnabledChange,
            onFlipToLockEnabledChange = onFlipToLockEnabledChange,
            onFlipExitAndClearStackEnabledChange = onFlipExitAndClearStackEnabledChange
        )
        SettingsSectionTitle(
            text = stringResource(R.string.settings_privacy_sensitive_access_section)
        )
        RoundedGroup(
            items = listOf(
                switchSettingsGroupItem(
                    key = "privacy.reauthenticate_sensitive_copies",
                    icon = Icons.Default.VerifiedUser,
                    title = stringResource(
                        R.string.settings_security_reauthenticate_sensitive_copies
                    ),
                    subtitle = stringResource(
                        R.string.settings_security_reauthenticate_sensitive_copies_description
                    ),
                    checked = state.reauthenticateSensitiveCopies,
                    onCheckedChange = onSensitiveCopyReauthenticationChange
                ),
                switchSettingsGroupItem(
                    key = "privacy.clipboard_protection",
                    icon = Icons.Default.ContentPasteOff,
                    title = stringResource(R.string.settings_privacy_clipboard_protection),
                    subtitle = stringResource(
                        R.string.settings_privacy_clipboard_protection_description
                    ),
                    checked = state.clipboardClearEnabled,
                    onCheckedChange = onClipboardClearEnabledChange,
                ),
                dropdownSettingsGroupItem(
                    key = "privacy.clipboard_clear_delay",
                    title = stringResource(R.string.settings_privacy_clipboard_delay),
                    selected = state.clipboardClearDelaySeconds,
                    selectedLabel = stringResource(
                        R.string.settings_privacy_clipboard_delay_seconds,
                        state.clipboardClearDelaySeconds,
                    ),
                    options = delayOptions,
                    expanded = showClipboardDelayMenu,
                    onExpandedChange = { showClipboardDelayMenu = it },
                    onSelect = onClipboardClearDelayChange,
                ),
                settingsGroupItem(
                    key = "privacy.clear_clipboard_now",
                    title = stringResource(R.string.settings_privacy_clear_clipboard_now),
                    subtitle = stringResource(
                        R.string.settings_privacy_clear_clipboard_now_description
                    ),
                    onClick = onClearClipboard,
                )
            )
        )
    }
}
