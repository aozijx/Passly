package com.aozijx.passly.feature.settings.general

/** 通用设置页的页面级状态（MVI）。 */
data class GeneralSettingsUiState(
    val cacheSize: String? = null,
    val isCalculating: Boolean = true,
)

/** 通用设置页的页面级动作（MVI）：状态变更类交互。 */
sealed interface GeneralSettingsAction {
    data object RefreshCache : GeneralSettingsAction
    data object ClearCache : GeneralSettingsAction
}

/** 通用设置页的一次性效果（MVI）：缓存提示与页面级导航。 */
sealed interface GeneralSettingsEffect {
    data object CacheCleared : GeneralSettingsEffect
    data object OpenAppDetails : GeneralSettingsEffect
    data object OpenTerms : GeneralSettingsEffect
    data object OpenPrivacyPolicy : GeneralSettingsEffect
    data object OpenOpenSourceLicenses : GeneralSettingsEffect
}
