package com.aozijx.passly.ui.features.settings.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun RoundedGroup(
    modifier: Modifier = Modifier,
    itemSpacing: Dp = 2.dp,
    content: (RoundedGroupScope.() -> Unit)
) {
    val scope = remember {
        RoundedGroupScope()
    }.apply {
        items.clear()
        content()
    }

    Column(modifier = modifier.fillMaxWidth()) {
        val visibleItems = scope.items.withIndex()
            .filter { it.value.visible }
            .toList()

        scope.items.forEachIndexed { index, item ->
            val visibleIndex = visibleItems.indexOfFirst { it.index == index }
            val isLastVisible = visibleIndex == visibleItems.lastIndex && visibleIndex >= 0

            val position = if (visibleIndex >= 0) {
                when {
                    visibleItems.size == 1 -> GroupItemPosition.Single
                    visibleIndex == 0 -> GroupItemPosition.First
                    visibleIndex == visibleItems.lastIndex -> GroupItemPosition.Last
                    else -> GroupItemPosition.Middle
                }
            } else {
                GroupItemPosition.Middle
            }

            AnimatedVisibility(
                visible = item.visible,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column {
                    item.content(position)
                    if (!isLastVisible) {
                        Spacer(modifier = Modifier.height(itemSpacing))
                    }
                }
            }
        }
    }
}