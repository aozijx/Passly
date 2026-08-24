package com.aozijx.passly.presentation.ui.settings.autofill

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.aozijx.passly.R
import com.aozijx.passly.core.ui.components.group.RoundedGroup
import com.aozijx.passly.core.ui.components.group.navigationSettingsGroupItem
import com.aozijx.passly.core.ui.components.group.sliderSettingsGroupItem
import com.aozijx.passly.core.ui.components.group.switchSettingsGroupItem
import com.aozijx.passly.core.ui.components.settings.SettingsSectionTitle
import com.aozijx.passly.presentation.ui.settings.autofill.model.AutofillPresentationUiModel
import com.aozijx.passly.presentation.ui.settings.autofill.model.AutofillSettingsUiModel
import kotlin.math.roundToInt

@Composable
internal fun AutofillSettingsSection(
    settings: AutofillSettingsUiModel,
    onOpenAutofillSettings: () -> Unit,
    onEnabledChange: (Boolean) -> Unit,
    onPresentationChange: (AutofillPresentationUiModel) -> Unit,
    onCredentialManagerEnabledChange: (Boolean) -> Unit,
    onAuthenticationRequiredChange: (Boolean) -> Unit,
    onOtpEnabledChange: (Boolean) -> Unit,
    onSavePromptsEnabledChange: (Boolean) -> Unit,
    onUnmatchedSuggestionsEnabledChange: (Boolean) -> Unit,
    onMaxSuggestionsChange: (Int) -> Unit,
) {
    var candidateLimit by remember {
        mutableFloatStateOf(settings.maxSuggestions.toFloat())
    }
    LaunchedEffect(settings.maxSuggestions) {
        candidateLimit = settings.maxSuggestions.toFloat()
    }

    val enabled = settings.enabled
    SettingsSectionTitle(text = stringResource(R.string.settings_autofill_section))
    RoundedGroup(
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
                    onClick = onOpenAutofillSettings,
                )
            )
            add(
                switchSettingsGroupItem(
                    key = "autofill.enabled",
                    title = stringResource(R.string.settings_autofill_enabled),
                    subtitle = stringResource(R.string.settings_autofill_enabled_summary),
                    checked = settings.enabled,
                    onCheckedChange = onEnabledChange,
                )
            )
            add(
                navigationSettingsGroupItem(
                    key = "autofill.presentation",
                    enabled = enabled,
                    title = stringResource(R.string.settings_autofill_presentation),
                    value = stringResource(
                        when (settings.presentation) {
                            AutofillPresentationUiModel.SYSTEM_INLINE ->
                                R.string.settings_autofill_mode_inline

                            AutofillPresentationUiModel.BOTTOM_SHEET ->
                                R.string.settings_autofill_mode_bottom_sheet
                        }
                    ),
                    onClick = {
                        val next = when (settings.presentation) {
                            AutofillPresentationUiModel.SYSTEM_INLINE ->
                                AutofillPresentationUiModel.BOTTOM_SHEET

                            AutofillPresentationUiModel.BOTTOM_SHEET ->
                                AutofillPresentationUiModel.SYSTEM_INLINE
                        }
                        onPresentationChange(next)
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
                    onCheckedChange = onCredentialManagerEnabledChange,
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
                    onCheckedChange = onAuthenticationRequiredChange,
                )
            )
            add(
                switchSettingsGroupItem(
                    key = "autofill.otp",
                    enabled = enabled,
                    title = stringResource(R.string.settings_autofill_include_otp),
                    subtitle = stringResource(R.string.settings_autofill_include_otp_summary),
                    checked = settings.includeOtp,
                    onCheckedChange = onOtpEnabledChange,
                )
            )
            add(
                switchSettingsGroupItem(
                    key = "autofill.save",
                    enabled = enabled,
                    title = stringResource(R.string.settings_autofill_save_prompts),
                    subtitle = stringResource(R.string.settings_autofill_save_prompts_summary),
                    checked = settings.savePromptsEnabled,
                    onCheckedChange = onSavePromptsEnabledChange,
                )
            )
            add(
                switchSettingsGroupItem(
                    key = "autofill.unmatched",
                    enabled = enabled,
                    title = stringResource(R.string.settings_autofill_unmatched),
                    subtitle = stringResource(R.string.settings_autofill_unmatched_summary),
                    checked = settings.allowUnmatchedSuggestions,
                    onCheckedChange = onUnmatchedSuggestionsEnabledChange,
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
                        onMaxSuggestionsChange(candidateLimit.roundToInt())
                    },
                )
            )
        }
    )
}
