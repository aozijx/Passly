package com.aozijx.passly.data.repository.settings

import com.aozijx.passly.data.local.datastore.settings.InterfacePreferences
import com.aozijx.passly.domain.settings.model.AppCornerRadiusConstraints
import com.aozijx.passly.domain.settings.model.InterfaceSettings

internal fun readInterface(p: InterfacePreferences): InterfaceSettings =
    InterfaceSettings(
        hideSystemBars = p.hideSystemBars,
        collapseTopBarOnScroll = p.collapseTopBarOnScroll,
        collapseQuickFilterBarOnScroll = p.collapseQuickFilterBarOnScroll,
        appCornerRadiusDp = AppCornerRadiusConstraints.normalize(p.appCornerRadiusDp),
    )
