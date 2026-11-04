package com.aozijx.passly.presentation.shared.components.group

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import com.aozijx.passly.presentation.shared.components.group.model.SegmentedSettingsItem
import com.aozijx.passly.presentation.shared.components.menu.MenuOptionText
import com.aozijx.passly.presentation.shared.components.menu.selectedMenuModifier
import com.aozijx.passly.presentation.shared.util.rememberClickThrottle

private val SelectionMenuMinimumWidth = 112.dp
private val SelectionMenuMaximumWidth = 280.dp
private val SelectionMenuVerticalPadding = 8.dp
private const val SELECTION_MENU_MAXIMUM_SCREEN_HEIGHT_RATIO = 0.75f

/** Popup behavior shared by compact selection controls. */
@Composable
fun <T> AnchoredSelectionMenu(
    selected: T,
    options: List<Pair<T, String>>,
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    anchor: @Composable () -> Unit,
) {
    Box(modifier = modifier) {
        anchor()
        SelectionPopup(
            selected = selected,
            options = options,
            expanded = expanded,
            onDismissRequest = onDismissRequest,
            onSelect = onSelect,
        )
    }
}

/** Adapts the anchored menu to the standard segmented settings row. */
@Composable
fun <T> anchoredSelectionSettingsItem(
    key: String,
    title: String,
    selected: T,
    selectedLabel: String,
    options: List<Pair<T, String>>,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onSelect: (T) -> Unit,
    icon: ImageVector? = null,
    enabled: Boolean = true,
): SegmentedSettingsItem =
    SegmentedSettingsItem(key = key) { shapes ->
        val allowToggle = rememberClickThrottle()
        SegmentedListItem(
            onClick = { if (enabled && allowToggle.tryAcquire()) onExpandedChange(!expanded) },
            shapes = shapes,
            colors = settingsSegmentedColors(),
            enabled = enabled,
            verticalAlignment = Alignment.CenterVertically,
            leadingContent = icon.asLeadingContent(),
            content = { Text(title) },
            trailingContent = {
                AnchoredSelectionMenu(
                    selected = selected,
                    options = options,
                    expanded = expanded,
                    onDismissRequest = { onExpandedChange(false) },
                    onSelect = onSelect,
                    anchor = {
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
                        )
                    },
                )
            },
        )
    }

@Composable
private fun <T> SelectionPopup(
    selected: T,
    options: List<Pair<T, String>>,
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    onSelect: (T) -> Unit,
) {
    val visibility = remember { MutableTransitionState(false) }
    visibility.targetState = expanded

    if (expanded || visibility.currentState) {
        Popup(
            popupPositionProvider = RightAlignedPopupPositionProvider,
            onDismissRequest = onDismissRequest,
            properties = PopupProperties(focusable = expanded),
        ) {
            val maxHeight =
                LocalConfiguration.current.screenHeightDp.dp * SELECTION_MENU_MAXIMUM_SCREEN_HEIGHT_RATIO
            AnimatedVisibility(
                visibleState = visibility,
                enter = expandVertically(expandFrom = Alignment.Top) + fadeIn(),
                exit = shrinkVertically(shrinkTowards = Alignment.Top) + fadeOut(),
            ) {
                Surface(
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    tonalElevation = 6.dp,
                    shadowElevation = 6.dp,
                ) {
                    Column(
                        modifier =
                            Modifier
                                .width(IntrinsicSize.Max)
                                .widthIn(
                                    min = SelectionMenuMinimumWidth,
                                    max = SelectionMenuMaximumWidth,
                                ).heightIn(max = maxHeight)
                                .verticalScroll(rememberScrollState())
                                .padding(vertical = SelectionMenuVerticalPadding),
                    ) {
                        options.forEach { (value, label) ->
                            val isSelected = value == selected
                            DropdownMenuItem(
                                text = { MenuOptionText(text = label, selected = isSelected) },
                                onClick = {
                                    onSelect(value)
                                    onDismissRequest()
                                },
                                modifier = Modifier.selectedMenuModifier(isSelected),
                            )
                        }
                    }
                }
            }
        }
    }
}

private object RightAlignedPopupPositionProvider : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset {
        val maxX = (windowSize.width - popupContentSize.width).coerceAtLeast(0)
        val x = (anchorBounds.right - popupContentSize.width).coerceIn(0, maxX)
        val below = anchorBounds.bottom
        val above = anchorBounds.top - popupContentSize.height
        val y =
            if (below + popupContentSize.height <= windowSize.height || above < 0) {
                below.coerceAtMost((windowSize.height - popupContentSize.height).coerceAtLeast(0))
            } else {
                above
            }
        return IntOffset(x, y)
    }
}
