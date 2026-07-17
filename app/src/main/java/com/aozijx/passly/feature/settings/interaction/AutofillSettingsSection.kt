package com.aozijx.passly.feature.settings.interaction

import androidx.compose.runtime.Composable
import com.aozijx.passly.domain.model.settings.AutofillUiMode
import com.aozijx.passly.feature.settings.components.navigationSettingsItem
import com.aozijx.passly.feature.settings.shell.SettingsGroupTitle
import com.aozijx.passly.feature.settings.shell.SettingsRoundedGroup

@Composable
internal fun AutofillSettingsSection(
    autofillUiMode: AutofillUiMode,
    onToggleAutofillUiMode: () -> Unit
) {
    SettingsGroupTitle(text = "自动填充模式")
    SettingsRoundedGroup {
        navigationSettingsItem(
            title = "填充方式",
            value = when (autofillUiMode) {
                AutofillUiMode.SYSTEM_INLINE -> "键盘候选"
                AutofillUiMode.BOTTOM_SHEET -> "底部弹窗"
            },
            onClick = onToggleAutofillUiMode
        )
    }
}