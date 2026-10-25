package com.aozijx.passly.feature.settings.presentation.ui.autofill

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.foundation.layout.Column
import com.aozijx.passly.R
import com.aozijx.passly.domain.settings.model.AutofillPresentation
import com.aozijx.passly.feature.settings.presentation.autofill.AutofillSettingsAction
import com.aozijx.passly.presentation.shared.components.group.SegmentedSettingsGroup
import com.aozijx.passly.presentation.shared.components.group.navigationSettingsGroupItem
import com.aozijx.passly.presentation.shared.components.group.sliderSettingsGroupItem
import com.aozijx.passly.presentation.shared.components.group.switchSettingsGroupItem
import com.aozijx.passly.core.ui.components.settings.SettingsSectionTitle
import com.aozijx.passly.feature.settings.presentation.ui.autofill.model.AutofillSettingsUiModel
import kotlin.math.roundToInt

@Composable
internal fun AutofillSettingsSection(
    settings: AutofillSettingsUiModel,
    onAction: (AutofillSettingsAction) -> Unit,
) {
    var candidateLimit by remember {
        mutableFloatStateOf(settings.maxSuggestions.toFloat())
    }
    var showPresentationDialog by remember { mutableStateOf(false) }
    LaunchedEffect(settings.maxSuggestions) {
        candidateLimit = settings.maxSuggestions.toFloat()
    }

    val enabled = settings.enabled
    SettingsSectionTitle(text = stringResource(R.string.settings_autofill_section))
    SegmentedSettingsGroup(
        items = buildList {
            add(
                navigationSettingsGroupItem(
                    key = "autofill.system_settings",
                    title = stringResource(R.string.settings_autofill_system_service),
                    subtitle = stringResource(R.string.settings_autofill_system_service_summary),
                    value = stringResource(
                        if (settings.isSystemServiceEnabled) {
                            R.string.settings_autofill_system_enabled
                        } else {
                            R.string.settings_autofill_system_disabled
                        }
                    ),
                    onClick = { onAction(AutofillSettingsAction.OpenSystemAutofillSettings) },
                )
            )
            add(
                switchSettingsGroupItem(
                    key = "autofill.enabled",
                    title = stringResource(R.string.settings_autofill_enabled),
                    subtitle = stringResource(R.string.settings_autofill_enabled_summary),
                    checked = settings.enabled,
                    onCheckedChange = { onAction(AutofillSettingsAction.SetEnabled(it)) },
                )
            )
            add(
                navigationSettingsGroupItem(
                    key = "autofill.accessibility",
                    title = stringResource(R.string.settings_autofill_accessibility),
                    subtitle = stringResource(R.string.settings_autofill_accessibility_summary),
                    onClick = { onAction(AutofillSettingsAction.OpenAccessibilitySettings) },
                )
            )
            add(
                navigationSettingsGroupItem(
                    key = "autofill.presentation",
                    enabled = enabled,
                    title = stringResource(R.string.settings_autofill_presentation),
                    value = stringResource(
                        when (settings.presentation) {
                            AutofillPresentation.FLOATING ->
                                R.string.settings_autofill_mode_floating
                            AutofillPresentation.SYSTEM_INLINE ->
                                R.string.settings_autofill_mode_inline

                            AutofillPresentation.BOTTOM_SHEET ->
                                R.string.settings_autofill_mode_bottom_sheet
                        }
                    ),
                    onClick = {
                        showPresentationDialog = true
                    },
                )
            )
            add(
                switchSettingsGroupItem(
                    key = "autofill.credential_manager_enabled",
                    visible = settings.supportsCredentialManager,
                    enabled = enabled,
                    title = stringResource(R.string.settings_autofill_credential_manager),
                    subtitle = stringResource(
                        R.string.settings_autofill_credential_manager_summary
                    ),
                    checked = settings.credentialManagerEnabled,
                    onCheckedChange = {
                        onAction(AutofillSettingsAction.SetCredentialManagerEnabled(it))
                    },
                )
            )
            add(
                switchSettingsGroupItem(
                    key = "autofill.authentication",
                    enabled = enabled,
                    title = stringResource(R.string.settings_autofill_require_authentication),
                    subtitle = stringResource(
                        R.string.settings_autofill_require_authentication_summary
                    ),
                    checked = settings.requireAuthentication,
                    onCheckedChange = {
                        onAction(AutofillSettingsAction.SetAuthenticationRequired(it))
                    },
                )
            )
            add(
                switchSettingsGroupItem(
                    key = "autofill.otp",
                    enabled = enabled,
                    title = stringResource(R.string.settings_autofill_include_otp),
                    subtitle = stringResource(R.string.settings_autofill_include_otp_summary),
                    checked = settings.includeOtp,
                    onCheckedChange = { onAction(AutofillSettingsAction.SetOtpEnabled(it)) },
                )
            )
            add(
                switchSettingsGroupItem(
                    key = "autofill.save_prompts",
                    enabled = enabled,
                    title = stringResource(R.string.settings_autofill_save_prompts),
                    subtitle = stringResource(R.string.settings_autofill_save_prompts_summary),
                    checked = settings.savePromptsEnabled,
                    onCheckedChange = {
                        onAction(AutofillSettingsAction.SetSavePromptsEnabled(it))
                    },
                )
            )
            add(
                switchSettingsGroupItem(
                    key = "autofill.unmatched",
                    enabled = enabled,
                    title = stringResource(R.string.settings_autofill_unmatched),
                    subtitle = stringResource(R.string.settings_autofill_unmatched_summary),
                    checked = settings.allowUnmatchedSuggestions,
                    onCheckedChange = {
                        onAction(AutofillSettingsAction.SetUnmatchedSuggestionsEnabled(it))
                    },
                )
            )
            add(
                sliderSettingsGroupItem(
                    key = "autofill.candidate_limit",
                    enabled = enabled,
                    title = stringResource(R.string.settings_autofill_candidate_limit),
                    subtitle = stringResource(
                        R.string.settings_autofill_candidate_limit_summary
                    ),
                    value = candidateLimit,
                    valueLabel = candidateLimit.roundToInt().toString(),
                    valueRange = settings.minSuggestions.toFloat()..
                            settings.maxSuggestionsLimit.toFloat(),
                    steps = settings.maxSuggestionsLimit - settings.minSuggestions - 1,
                    onValueChange = { candidateLimit = it },
                    onValueChangeFinished = {
                        onAction(
                            AutofillSettingsAction.SetMaxSuggestions(
                                candidateLimit.roundToInt(),
                            ),
                        )
                    },
                )
            )
        }
    )
    if (showPresentationDialog) {
        AlertDialog(
            onDismissRequest = { showPresentationDialog = false },
            title = { Text(stringResource(R.string.settings_autofill_presentation)) },
            text = {
                Column {
                    AutofillPresentation.entries.forEach { mode ->
                        val label = when (mode) {
                            AutofillPresentation.FLOATING -> R.string.settings_autofill_mode_floating
                            AutofillPresentation.SYSTEM_INLINE -> R.string.settings_autofill_mode_inline
                            AutofillPresentation.BOTTOM_SHEET -> R.string.settings_autofill_mode_bottom_sheet
                        }
                        TextButton(onClick = {
                            onAction(AutofillSettingsAction.SetPresentation(mode))
                            showPresentationDialog = false
                        }) { Text(stringResource(label)) }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showPresentationDialog = false }) {
                    Text(stringResource(android.R.string.cancel))
                }
            },
        )
    }
}
