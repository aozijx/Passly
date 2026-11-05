package com.aozijx.passly.presentation.shared.components.group

import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
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
import kotlin.math.roundToInt

private val SELECTION_MENU_MINIMUM_WIDTH = 112.dp
private val SELECTION_MENU_MAXIMUM_WIDTH = 280.dp
private val SELECTION_MENU_VERTICAL_PADDING = 8.dp
private val SELECTION_MENU_SHADOW_OVERFLOW = 12.dp
private val SELECTION_MENU_REVEAL_OFFSET = 8.dp
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
    val motionScheme = MaterialTheme.motionScheme
    val menuAlpha by animateFloatAsState(
        targetValue = if (expanded) 1f else 0f,
        animationSpec = motionScheme.fastEffectsSpec(),
        label = "selectionMenuAlpha",
    )
    val revealOffset by animateFloatAsState(
        targetValue = if (expanded) 0f else 1f,
        animationSpec = motionScheme.defaultSpatialSpec(),
        label = "selectionMenuOffset",
    )
    val placement = remember { SelectionMenuPlacement() }
    val density = LocalDensity.current
    val shadowOverflowPx = with(density) { SELECTION_MENU_SHADOW_OVERFLOW.toPx() }
    val revealOffsetPx = with(density) { SELECTION_MENU_REVEAL_OFFSET.toPx() }

    if (expanded || menuAlpha > 0f) {
        val screenMaxHeight =
            LocalConfiguration.current.screenHeightDp.dp * SELECTION_MENU_MAXIMUM_SCREEN_HEIGHT_RATIO
        val maxHeight =
            if (placement.maxHeightPx > 0) {
                minOf(screenMaxHeight, with(density) { placement.maxHeightPx.toDp() })
            } else {
                screenMaxHeight
            }
        val opensAbove = placement.opensAbove

        Popup(
            popupPositionProvider =
                remember(placement, shadowOverflowPx) {
                    RightAlignedPopupPositionProvider(placement, shadowOverflowPx)
                },
            onDismissRequest = onDismissRequest,
            properties = PopupProperties(focusable = true),
        ) {
            Surface(
                modifier =
                    Modifier
                        // The padding is inside the animated layer and gives the shadow room, so
                        // fading or translating the menu cannot cut its shadow off.
                        .graphicsLayer {
                            alpha = menuAlpha
                            translationY = (if (opensAbove) 1f else -1f) * revealOffset * revealOffsetPx
                        }.padding(SELECTION_MENU_SHADOW_OVERFLOW),
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
                                min = SELECTION_MENU_MINIMUM_WIDTH,
                                max = SELECTION_MENU_MAXIMUM_WIDTH,
                            ).heightIn(max = maxHeight)
                            .verticalScroll(rememberScrollState())
                            .padding(vertical = SELECTION_MENU_VERTICAL_PADDING),
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

private class SelectionMenuPlacement {
    var opensAbove by mutableStateOf(false)
    var maxHeightPx by mutableIntStateOf(0)
}

/**
 * [shadowOverflowPx] is the shadow room baked into the popup content around the menu surface, so
 * the fades and the reveal offset never need to clip the shadow. The surface itself is placed
 * exactly against the anchor again by compensating that room here.
 */
private class RightAlignedPopupPositionProvider(
    private val placement: SelectionMenuPlacement,
    shadowOverflowPx: Float,
) : PopupPositionProvider {
    private val inset = shadowOverflowPx.roundToInt()

    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset {
        val surfaceWidth = (popupContentSize.width - inset * 2).coerceAtLeast(0)
        val surfaceHeight = (popupContentSize.height - inset * 2).coerceAtLeast(0)
        val surfaceLeft =
            (anchorBounds.right - surfaceWidth)
                .coerceIn(0, (windowSize.width - surfaceWidth).coerceAtLeast(0))
        val x = surfaceLeft - inset
        val spaceBelow = (windowSize.height - anchorBounds.bottom).coerceAtLeast(0)
        val spaceAbove = anchorBounds.top.coerceAtLeast(0)
        val opensAbove = surfaceHeight > spaceBelow && spaceAbove > spaceBelow
        val availableHeight = (if (opensAbove) spaceAbove else spaceBelow) - inset
        placement.opensAbove = opensAbove
        placement.maxHeightPx = availableHeight.coerceAtLeast(0)
        val y =
            if (opensAbove) {
                anchorBounds.top - surfaceHeight - inset
            } else {
                anchorBounds.bottom - inset
            }
        return IntOffset(x, y)
    }
}
