package com.aozijx.passly.feature.settings.interaction

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.aozijx.passly.R
import com.aozijx.passly.core.ui.components.group.RoundedGroup
import com.aozijx.passly.core.ui.components.group.navigationSettingsGroupItem
import com.aozijx.passly.core.ui.components.settings.SettingsSectionTitle
import com.aozijx.passly.domain.settings.model.AutofillUiMode

@Composable
internal fun AutofillSettingsSection(
    autofillUiMode: AutofillUiMode,
    onOpenAutofillSettings: () -> Unit,
    onToggleAutofillUiMode: () -> Unit
) {
    SettingsSectionTitle(text = stringResource(R.string.settings_autofill_mode_section))
    RoundedGroup(
        items = listOf(
            navigationSettingsGroupItem(
                key = "interaction.autofill_settings",
                title = stringResource(R.string.settings_vault_enable_autofill),
                subtitle = stringResource(R.string.settings_vault_toast_enable_autofill_manual),
                onClick = onOpenAutofillSettings
            ),
            navigationSettingsGroupItem(
                key = "interaction.autofill_mode",
                title = stringResource(R.string.settings_autofill_mode_title),
                value = stringResource(
                    when (autofillUiMode) {
                        AutofillUiMode.SYSTEM_INLINE -> R.string.settings_autofill_mode_inline
                        AutofillUiMode.BOTTOM_SHEET -> R.string.settings_autofill_mode_bottom_sheet
                    }
                ),
                onClick = onToggleAutofillUiMode
            )
        )
    )
}
