package com.aozijx.passly.feature.settings.interaction

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.aozijx.passly.R
import com.aozijx.passly.domain.settings.model.AutofillUiMode
import com.aozijx.passly.ui.components.group.RoundedGroup
import com.aozijx.passly.ui.components.group.navigationSettingsGroupItem
import com.aozijx.passly.ui.components.settings.SettingsSectionTitle

@Composable
internal fun AutofillSettingsSection(
    autofillUiMode: AutofillUiMode,
    onOpenAutofillSettings: () -> Unit,
    onToggleAutofillUiMode: () -> Unit
) {
    SettingsSectionTitle(text = "自动填充模式")
    RoundedGroup(
        items = listOf(
            navigationSettingsGroupItem(
                key = "interaction.autofill_settings",
                title = stringResource(R.string.vault_menu_enable_autofill),
                subtitle = stringResource(R.string.vault_toast_enable_autofill_manual),
                onClick = onOpenAutofillSettings
            ),
            navigationSettingsGroupItem(
                key = "interaction.autofill_mode",
                title = "填充方式",
                value = when (autofillUiMode) {
                    AutofillUiMode.SYSTEM_INLINE -> "键盘候选"
                    AutofillUiMode.BOTTOM_SHEET -> "底部弹窗"
                },
                onClick = onToggleAutofillUiMode
            )
        )
    )
}
