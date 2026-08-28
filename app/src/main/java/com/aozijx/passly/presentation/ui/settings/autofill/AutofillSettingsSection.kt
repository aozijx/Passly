package com.aozijx.passly.presentation.ui.settings.autofill

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.aozijx.passly.R
import com.aozijx.passly.presentation.ui.shared.components.group.RoundedGroup
import com.aozijx.passly.presentation.ui.shared.components.group.navigationSettingsGroupItem
import com.aozijx.passly.presentation.ui.shared.components.group.sliderSettingsGroupItem
import com.aozijx.passly.presentation.ui.shared.components.group.switchSettingsGroupItem
import com.aozijx.passly.core.ui.components.settings.SettingsSectionTitle
import com.aozijx.passly.presentation.ui.settings.autofill.model.AutofillPresentationUiModel
import com.aozijx.passly.presentation.ui.settings.autofill.model.AutofillSettingsEventHandler
import com.aozijx.passly.presentation.ui.settings.autofill.model.AutofillSettingsUiModel
import kotlin.math.roundToInt

@Composable
internal fun AutofillSettingsSection(
    settings: AutofillSettingsUiModel,
    eventHandler: AutofillSettingsEventHandler,
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
                    onClick = eventHandler::onOpenSystemSettings,
                )
            )
            add(
                switchSettingsGroupItem(
                    key = "autofill.enabled",
                    title = stringResource(R.string.settings_autofill_enabled),
                    subtitle = stringResource(R.string.settings_autofill_enabled_summary),
                    checked = settings.enabled,
                    onCheckedChange = eventHandler::onEnabledChanged,
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
                        eventHandler.onPresentationChanged(next)
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
                    onCheckedChange = eventHandler::onCredentialManagerEnabledChanged,
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
                    onCheckedChange = eventHandler::onAuthenticationRequiredChanged,
                )
            )
            add(
                switchSettingsGroupItem(
                    key = "autofill.otp",
                    enabled = enabled,
                    title = stringResource(R.string.settings_autofill_include_otp),
                    subtitle = stringResource(R.string.settings_autofill_include_otp_summary),
                    checked = settings.includeOtp,
                    onCheckedChange = eventHandler::onOtpEnabledChanged,
                )
            )
            add(
                switchSettingsGroupItem(
                    key = "autofill.save",
                    enabled = enabled,
                    title = stringResource(R.string.settings_autofill_save_prompts),
                    subtitle = stringResource(R.string.settings_autofill_save_prompts_summary),
                    checked = settings.savePromptsEnabled,
                    onCheckedChange = eventHandler::onSavePromptsEnabledChanged,
                )
            )
            add(
                switchSettingsGroupItem(
                    key = "autofill.unmatched",
                    enabled = enabled,
                    title = stringResource(R.string.settings_autofill_unmatched),
                    subtitle = stringResource(R.string.settings_autofill_unmatched_summary),
                    checked = settings.allowUnmatchedSuggestions,
                    onCheckedChange = eventHandler::onUnmatchedSuggestionsEnabledChanged,
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
                        eventHandler.onMaxSuggestionsChanged(candidateLimit.roundToInt())
                    },
                )
            )
        }
    )
}
