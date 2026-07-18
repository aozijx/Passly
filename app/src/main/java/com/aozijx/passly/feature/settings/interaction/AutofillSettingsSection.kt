package com.aozijx.passly.feature.settings.interaction

import androidx.compose.runtime.Composable
import com.aozijx.passly.domain.model.settings.AutofillUiMode
import com.aozijx.passly.ui.components.group.RoundedGroup
import com.aozijx.passly.ui.components.group.navigationSettingsGroupItem
import com.aozijx.passly.ui.components.settings.SettingsSectionTitle

@Composable
internal fun AutofillSettingsSection(
    autofillUiMode: AutofillUiMode,
    onToggleAutofillUiMode: () -> Unit
) {
    SettingsSectionTitle(text = "自动填充模式")
    RoundedGroup(
        items = listOf(
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
