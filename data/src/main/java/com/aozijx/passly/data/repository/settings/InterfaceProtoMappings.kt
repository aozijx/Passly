package com.aozijx.passly.data.repository.settings

import com.aozijx.passly.data.local.datastore.settings.InterfacePreferences
import com.aozijx.passly.domain.settings.model.InterfaceSettings
import com.aozijx.passly.domain.settings.model.InterfaceStyleConstraints

internal fun readInterface(p: InterfacePreferences): InterfaceSettings =
    InterfaceSettings(
        hideSystemBars = p.hideSystemBars,
        collapseTopBarOnScroll = p.collapseTopBarOnScroll,
        collapseQuickFilterBarOnScroll = p.collapseQuickFilterBarOnScroll,
        outerCornerRadiusDp = p.outerCornerRadiusDp.coerceIn(
            InterfaceStyleConstraints.MIN_OUTER_RADIUS_DP,
            InterfaceStyleConstraints.MAX_OUTER_RADIUS_DP
        ),
        innerCornerRadiusDp = p.innerCornerRadiusDp.coerceIn(
            InterfaceStyleConstraints.MIN_INNER_RADIUS_DP,
            InterfaceStyleConstraints.MAX_INNER_RADIUS_DP
        ),
        groupItemSpacingDp = p.groupItemSpacingDp.coerceIn(
            InterfaceStyleConstraints.MIN_ITEM_SPACING_DP,
            InterfaceStyleConstraints.MAX_ITEM_SPACING_DP
        ),
        groupContentPaddingDp = p.groupContentPaddingDp.coerceIn(
            InterfaceStyleConstraints.MIN_CONTENT_PADDING_DP,
            InterfaceStyleConstraints.MAX_CONTENT_PADDING_DP
        )
    )
