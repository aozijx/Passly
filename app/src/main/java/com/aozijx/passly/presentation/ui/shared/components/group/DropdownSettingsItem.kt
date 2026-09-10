package com.aozijx.passly.presentation.ui.shared.components.group

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.aozijx.passly.presentation.ui.shared.components.group.model.SegmentedSettingsItem
import com.aozijx.passly.presentation.ui.shared.components.menu.MenuOptionText
import com.aozijx.passly.presentation.ui.shared.components.menu.selectedMenuModifier

/**
 * 下拉选择设置项：整行点击展开/收起（toggle），选中项以背景高亮标识。
 */
@Composable
fun <T> dropdownSettingsGroupItem(
    key: String,
    icon: ImageVector? = null,
    title: String,
    selected: T,
    selectedLabel: String,
    options: List<Pair<T, String>>,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onSelect: (T) -> Unit
): SegmentedSettingsItem = SegmentedSettingsItem(key = key) { shapes ->
    Box {
        SegmentedListItem(
            onClick = { onExpandedChange(!expanded) },
            shapes = shapes,
            colors = settingsSegmentedColors(),
            verticalAlignment = Alignment.CenterVertically,
            leadingContent = icon.asLeadingContent(),
            trailingContent = {
                AnimatedSettingValue(
                    targetState = selected,
                    valueLabel = { target ->
                        options.firstOrNull { (value, _) -> value == target }?.second
                            ?: selectedLabel
                    },
                    transitionDirection = { initial, target ->
                        options.indexOfFirst { (value, _) -> value == target } -
                            options.indexOfFirst { (value, _) -> value == initial }
                    },
                    modifier = Modifier.padding(horizontal = 8.dp),
                )
            },
            content = {
                Text(text = title)
            },
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandedChange(false) }
        ) {
            options.forEach { (value, label) ->
                val isSelected = value == selected
                DropdownMenuItem(
                    text = {
                        MenuOptionText(
                            text = label,
                            selected = isSelected
                        )
                    },
                    onClick = {
                        onSelect(value)
                        onExpandedChange(false)
                    },
                    modifier = Modifier.selectedMenuModifier(isSelected)
                )
            }
        }
    }
}
