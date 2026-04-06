package com.aozijx.passly.features.settings.components.sections

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aozijx.passly.core.designsystem.model.VaultCardStyle
import com.aozijx.passly.features.settings.components.common.ClickableSettingItem
import com.aozijx.passly.features.settings.components.common.SettingsCard
import com.aozijx.passly.features.settings.components.common.SettingsGroupTitle

@Composable
fun AppearanceCustomizationSettingsSection(
    availableStyles: List<VaultCardStyle>,
    passwordSelectedStyle: VaultCardStyle,
    totpSelectedStyle: VaultCardStyle,
    onPasswordStyleSelected: (VaultCardStyle) -> Unit,
    onTotpStyleSelected: (VaultCardStyle) -> Unit
) {
    SettingsGroupTitle(text = "外观定制")
    SettingsCard {
        ClickableSettingItem(
            icon = Icons.Default.Palette, title = "个性化配色", value = "动态取色", onClick = {})
        HorizontalDivider(Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp)
        CardStyleSettingsSection(
            availableStyles = availableStyles,
            passwordSelectedStyle = passwordSelectedStyle,
            totpSelectedStyle = totpSelectedStyle,
            onPasswordStyleSelected = onPasswordStyleSelected,
            onTotpStyleSelected = onTotpStyleSelected
        )
    }
}
