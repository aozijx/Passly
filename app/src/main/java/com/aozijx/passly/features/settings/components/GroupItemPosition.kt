package com.aozijx.passly.features.settings.components

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class GroupItemPosition {
    Single,
    First,
    Middle,
    Last
}

fun GroupItemPosition.shape(
    radius: Dp = 18.dp,
    innerRadius: Dp = 2.dp
): RoundedCornerShape {
    return when (this) {
        GroupItemPosition.Single ->
            RoundedCornerShape(radius)

        GroupItemPosition.First ->
            RoundedCornerShape(
                topStart = radius, topEnd = radius,
                bottomStart = innerRadius, bottomEnd = innerRadius
            )

        GroupItemPosition.Middle ->
            RoundedCornerShape(innerRadius)

        GroupItemPosition.Last ->
            RoundedCornerShape(
                topStart = innerRadius, topEnd = innerRadius,
                bottomStart = radius, bottomEnd = radius
            )
    }
}