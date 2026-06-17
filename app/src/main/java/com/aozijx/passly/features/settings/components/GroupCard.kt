package com.aozijx.passly.features.settings.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun GroupCard(
    position: GroupItemPosition,
    modifier: Modifier = Modifier,
    radius: Dp = 18.dp,
    innerRadius: Dp = 2.dp,
    backgroundColor: Color = MaterialTheme.colorScheme.surfaceContainer,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val shape = remember(position, radius, innerRadius) {
        position.shape(radius, innerRadius)
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = shape,
        color = backgroundColor,
        enabled = enabled && onClick != null,
        onClick = onClick ?: {},
        content = {
            Column(
                modifier = Modifier.padding(contentPadding)
            ) {
                content()
            }
        }
    )
}