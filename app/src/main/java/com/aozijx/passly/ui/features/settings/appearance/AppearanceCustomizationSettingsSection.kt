package com.aozijx.passly.ui.features.settings.appearance

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Palette
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.aozijx.passly.domain.model.VaultCardStyle
import com.aozijx.passly.ui.features.settings.components.GroupCard
import com.aozijx.passly.ui.features.settings.components.RoundedGroupScope
import com.aozijx.passly.ui.features.settings.components.navigationSettingsItem
import com.aozijx.passly.ui.features.settings.shell.SettingsGroupTitle
import com.aozijx.passly.ui.features.settings.shell.SettingsRoundedGroup

@Composable
fun AppearanceCustomizationSettingsSection(
    availableStyles: List<VaultCardStyle>,
    passwordSelectedStyle: VaultCardStyle,
    totpSelectedStyle: VaultCardStyle,
    onPasswordStyleSelected: (VaultCardStyle) -> Unit,
    onTotpStyleSelected: (VaultCardStyle) -> Unit
) {
    SettingsGroupTitle(text = "外观定制")
    SettingsRoundedGroup {
        navigationSettingsItem(
            icon = Icons.Default.Palette, title = "个性化配色", value = "动态取色", onClick = {})
        RoundedGroupScope.item { position ->
            GroupCard(position = position, contentPadding = PaddingValues(0.dp)) {
                CardStyleSettingsSection(
                    availableStyles = availableStyles,
                    passwordSelectedStyle = passwordSelectedStyle,
                    totpSelectedStyle = totpSelectedStyle,
                    onPasswordStyleSelected = onPasswordStyleSelected,
                    onTotpStyleSelected = onTotpStyleSelected
                )
            }
        }
    }
}