package com.aozijx.passly.domain.settings.model

data class InterfaceSettings(
    val hideSystemBars: Boolean = false,
    val collapseTopBarOnScroll: Boolean = false,
    val collapseQuickFilterBarOnScroll: Boolean = false,
    val appCornerRadiusDp: Float = AppCornerRadiusConstraints.DEFAULT_DP,
)
