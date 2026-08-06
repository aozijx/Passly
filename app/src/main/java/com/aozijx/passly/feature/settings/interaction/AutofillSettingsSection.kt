package com.aozijx.passly.feature.settings.interaction

import android.os.Build
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
import com.aozijx.passly.domain.settings.model.AutofillPresentation
import com.aozijx.passly.domain.settings.model.AutofillSettings
import kotlin.math.roundToInt

@Composable
internal fun AutofillSettingsSection(
    settings: AutofillSettings,
    isSystemServiceEnabled: Boolean,
    onOpenAutofillSettings: () -> Unit,
    onAction: (InteractionSettingsAction) -> Unit,
) {
    var candidateLimit by remember {
        mutableFloatStateOf(settings.normalizedMaxSuggestions.toFloat())
    }
    LaunchedEffect(settings.maxSuggestions) {
        candidateLimit = settings.normalizedMaxSuggestions.toFloat()
    }

    SettingsSectionTitle(text = stringResource(R.string.settings_autofill_section))
    RoundedGroup(
        items = buildList {
            add(
                navigationSettingsGroupItem(
                    key = "interaction.autofill_system_settings",
                    title = stringResource(R.string.settings_autofill_system_service),
                    subtitle = stringResource(R.string.settings_autofill_system_service_summary),
                    value = stringResource(
                        if (isSystemServiceEnabled) {
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
                    key = "interaction.autofill_enabled",
                    title = stringResource(R.string.settings_autofill_enabled),
                    subtitle = stringResource(R.string.settings_autofill_enabled_summary),
                    checked = settings.enabled,
                    onCheckedChange = {
                        onAction(InteractionSettingsAction.SetAutofillEnabled(it))
                    },
                )
            )
            if (settings.enabled) {
                add(
                    navigationSettingsGroupItem(
                        key = "interaction.autofill_presentation",
                        title = stringResource(R.string.settings_autofill_presentation),
                        value = stringResource(
                            when (settings.presentation) {
                                AutofillPresentation.SYSTEM_INLINE ->
                                    R.string.settings_autofill_mode_inline

                                AutofillPresentation.BOTTOM_SHEET ->
                                    R.string.settings_autofill_mode_bottom_sheet
                            }
                        ),
                        onClick = {
                            val next = when (settings.presentation) {
                                AutofillPresentation.SYSTEM_INLINE ->
                                    AutofillPresentation.BOTTOM_SHEET

                                AutofillPresentation.BOTTOM_SHEET ->
                                    AutofillPresentation.SYSTEM_INLINE
                            }
                            onAction(InteractionSettingsAction.SetAutofillPresentation(next))
                        },
                    )
                )
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    add(
                        switchSettingsGroupItem(
                            key = "interaction.credential_manager_enabled",
                            title = stringResource(R.string.settings_autofill_credential_manager),
                            subtitle = stringResource(
                                R.string.settings_autofill_credential_manager_summary
                            ),
                            checked = settings.credentialManagerEnabled,
                            onCheckedChange = {
                                onAction(InteractionSettingsAction.SetCredentialManagerEnabled(it))
                            },
                        )
                    )
                }
                add(
                    switchSettingsGroupItem(
                        key = "interaction.autofill_authentication",
                        title = stringResource(R.string.settings_autofill_require_authentication),
                        subtitle = stringResource(
                            R.string.settings_autofill_require_authentication_summary
                        ),
                        checked = settings.requireAuthentication,
                        onCheckedChange = {
                            onAction(
                                InteractionSettingsAction.SetAutofillAuthenticationRequired(it)
                            )
                        },
                    )
                )
                add(
                    switchSettingsGroupItem(
                        key = "interaction.autofill_otp",
                        title = stringResource(R.string.settings_autofill_include_otp),
                        subtitle = stringResource(R.string.settings_autofill_include_otp_summary),
                        checked = settings.includeOtp,
                        onCheckedChange = {
                            onAction(InteractionSettingsAction.SetAutofillOtpEnabled(it))
                        },
                    )
                )
                add(
                    switchSettingsGroupItem(
                        key = "interaction.autofill_save",
                        title = stringResource(R.string.settings_autofill_save_prompts),
                        subtitle = stringResource(R.string.settings_autofill_save_prompts_summary),
                        checked = settings.savePromptsEnabled,
                        onCheckedChange = {
                            onAction(
                                InteractionSettingsAction.SetAutofillSavePromptsEnabled(it)
                            )
                        },
                    )
                )
                add(
                    switchSettingsGroupItem(
                        key = "interaction.autofill_unmatched",
                        title = stringResource(R.string.settings_autofill_unmatched),
                        subtitle = stringResource(R.string.settings_autofill_unmatched_summary),
                        checked = settings.allowUnmatchedSuggestions,
                        onCheckedChange = {
                            onAction(
                                InteractionSettingsAction.SetUnmatchedAutofillSuggestionsEnabled(it)
                            )
                        },
                    )
                )
                add(
                    sliderSettingsGroupItem(
                        key = "interaction.autofill_candidate_limit",
                        title = stringResource(R.string.settings_autofill_candidate_limit),
                        subtitle = stringResource(
                            R.string.settings_autofill_candidate_limit_summary
                        ),
                        value = candidateLimit,
                        valueLabel = candidateLimit.roundToInt().toString(),
                        valueRange = AutofillSettings.MIN_SUGGESTIONS.toFloat()..
                                AutofillSettings.MAX_SUGGESTIONS.toFloat(),
                        steps = AutofillSettings.MAX_SUGGESTIONS -
                                AutofillSettings.MIN_SUGGESTIONS - 1,
                        onValueChange = { candidateLimit = it },
                        onValueChangeFinished = {
                            onAction(
                                InteractionSettingsAction.SetAutofillMaxSuggestions(
                                    candidateLimit.roundToInt()
                                )
                            )
                        },
                    )
                )
            }
        }
    )
}
