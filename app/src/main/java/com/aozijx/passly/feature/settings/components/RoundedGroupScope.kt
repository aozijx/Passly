package com.aozijx.passly.feature.settings.components

import androidx.compose.runtime.Composable

@DslMarker
annotation class RoundedGroupDsl

internal data class GroupItemData(
    val visible: Boolean = true,
    val content: @Composable (GroupItemPosition) -> Unit
)

@RoundedGroupDsl
class RoundedGroupScope {

    internal val items = mutableListOf<GroupItemData>()

    fun item(
        visible: Boolean = true,
        content: @Composable (GroupItemPosition) -> Unit
    ) {
        items += GroupItemData(visible = visible, content = content)
    }
}