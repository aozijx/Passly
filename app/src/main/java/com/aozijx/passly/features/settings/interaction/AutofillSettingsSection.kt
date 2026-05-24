package com.aozijx.passly.features.settings.interaction

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ViewDay
import androidx.compose.runtime.Composable
import com.aozijx.passly.domain.config.AutofillUiMode
import com.aozijx.passly.features.settings.shell.ClickableSettingItem
import com.aozijx.passly.features.settings.shell.SettingsCard
import com.aozijx.passly.features.settings.shell.SettingsGroupTitle

@Composable
fun AutofillSettingsSection(
    autofillUiMode: AutofillUiMode,
    onToggleAutofillUiMode: () -> Unit
) {
    SettingsGroupTitle(text = "自动填充")
    SettingsCard {
        ClickableSettingItem(
            icon = Icons.Default.ViewDay,
            title = "自动填充展示",
            value = when (autofillUiMode) {
                AutofillUiMode.SYSTEM_INLINE -> "键盘候选"
                AutofillUiMode.BOTTOM_SHEET -> "底部弹层"
            },
            onClick = onToggleAutofillUiMode
        )
    }
}