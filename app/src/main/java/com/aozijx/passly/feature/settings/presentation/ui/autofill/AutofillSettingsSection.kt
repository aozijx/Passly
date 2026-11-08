package com.aozijx.passly.feature.settings.presentation.ui.autofill

import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.aozijx.passly.R
import com.aozijx.passly.core.ui.components.settings.SettingsSectionTitle
import com.aozijx.passly.domain.settings.model.AutofillPresentation
import com.aozijx.passly.domain.settings.model.AutofillSettings
import com.aozijx.passly.feature.settings.presentation.autofill.AutofillSettingsAction
import com.aozijx.passly.feature.settings.presentation.autofill.AutofillSourceState
import com.aozijx.passly.presentation.shared.components.group.SegmentedSettingsGroup
import com.aozijx.passly.presentation.shared.components.group.anchoredSelectionSettingsItem
import com.aozijx.passly.presentation.shared.components.group.navigationSettingsGroupItem
import com.aozijx.passly.presentation.shared.components.group.switchSettingsGroupItem

@Composable
internal fun AutofillSettingsSection(
    settings: AutofillSettings,
    supportsCredentialManager: Boolean,
    systemService: AutofillSourceState<Boolean>,
    onAction: (AutofillSettingsAction) -> Unit,
) {
    var showPresentationMenu by remember { mutableStateOf(false) }

    val enabled = settings.enabled
    SettingsSectionTitle(text = stringResource(R.string.settings_autofill_section))
    if (systemService is AutofillSourceState.Failed) {
        Row {
            Text(stringResource(R.string.settings_autofill_system_status_unavailable))
            TextButton(onClick = { onAction(AutofillSettingsAction.RetrySystemService) }) {
                Text(stringResource(R.string.retry))
            }
        }
    }
    SegmentedSettingsGroup(
        items =
            buildList {
                add(
                    navigationSettingsGroupItem(
                        key = "autofill.system_settings",
                        title = stringResource(R.string.settings_autofill_system_service),
                        subtitle = stringResource(R.string.settings_autofill_system_service_summary),
                        value = stringResource(systemService.toDisplayString()),
                        onClick = { onAction(AutofillSettingsAction.OpenSystemAutofillSettings) },
                    ),
                )
                add(
                    switchSettingsGroupItem(
                        key = "autofill.enabled",
                        title = stringResource(R.string.settings_autofill_enabled),
                        subtitle = stringResource(R.string.settings_autofill_enabled_summary),
                        enabled = true,
                        checked = settings.enabled,
                        onCheckedChange = { onAction(AutofillSettingsAction.SetEnabled(it)) },
                    ),
                )
                add(
                    navigationSettingsGroupItem(
                        key = "autofill.accessibility",
                        title = stringResource(R.string.settings_autofill_accessibility),
                        subtitle = stringResource(R.string.settings_autofill_accessibility_summary),
                        onClick = { onAction(AutofillSettingsAction.OpenAccessibilitySettings) },
                    ),
                )
                add(
                    anchoredSelectionSettingsItem(
                        key = "autofill.presentation",
                        enabled = enabled,
                        selected = settings.presentation,
                        selectedLabel = stringResource(settings.presentation.labelRes),
                        options = AutofillPresentation.entries.map { it to stringResource(it.labelRes) },
                        expanded = showPresentationMenu,
                        onExpandedChange = { showPresentationMenu = it },
                        onSelect = { onAction(AutofillSettingsAction.SetPresentation(it)) },
                        title = stringResource(R.string.settings_autofill_presentation),
                    ),
                )
                add(
                    switchSettingsGroupItem(
                        key = "autofill.credential_manager_enabled",
                        visible = supportsCredentialManager,
                        enabled = enabled,
                        title = stringResource(R.string.settings_autofill_credential_manager),
                        subtitle =
                            stringResource(
                                R.string.settings_autofill_credential_manager_summary,
                            ),
                        checked = settings.credentialManagerEnabled,
                        onCheckedChange = {
                            onAction(AutofillSettingsAction.SetCredentialManagerEnabled(it))
                        },
                    ),
                )
                add(
                    switchSettingsGroupItem(
                        key = "autofill.authentication",
                        enabled = enabled,
                        title = stringResource(R.string.settings_autofill_require_authentication),
                        subtitle =
                            stringResource(
                                R.string.settings_autofill_require_authentication_summary,
                            ),
                        checked = settings.requireAuthentication,
                        onCheckedChange = {
                            onAction(AutofillSettingsAction.SetAuthenticationRequired(it))
                        },
                    ),
                )
                add(
                    switchSettingsGroupItem(
                        key = "autofill.strict_domain_matching",
                        enabled = enabled,
                        title = stringResource(R.string.settings_autofill_strict_domain_matching),
                        subtitle = stringResource(R.string.settings_autofill_strict_domain_matching_summary),
                        checked = settings.strictDomainMatching,
                        onCheckedChange = {
                            onAction(AutofillSettingsAction.SetStrictDomainMatching(it))
                        },
                    ),
                )
            },
    )
}

private val AutofillPresentation.labelRes: Int
    get() =
        when (this) {
            AutofillPresentation.FLOATING -> R.string.settings_autofill_mode_floating
            AutofillPresentation.SYSTEM_INLINE -> R.string.settings_autofill_mode_inline
            AutofillPresentation.BOTTOM_SHEET -> R.string.settings_autofill_mode_bottom_sheet
        }

private fun AutofillSourceState<Boolean>.toDisplayString(): Int =
    when (this) {
        is AutofillSourceState.Loading -> {
            previous?.toDisplayString() ?: R.string.settings_autofill_system_status_loading
        }

        is AutofillSourceState.Ready -> {
            value.toDisplayString()
        }

        is AutofillSourceState.Failed -> {
            R.string.settings_autofill_system_status_unavailable
        }
    }

private fun Boolean.toDisplayString(): Int =
    if (this) {
        R.string.settings_autofill_system_enabled
    } else {
        R.string.settings_autofill_system_disabled
    }
