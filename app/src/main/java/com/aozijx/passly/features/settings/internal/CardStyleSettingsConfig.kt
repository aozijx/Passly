package com.aozijx.passly.features.settings.internal

import com.aozijx.passly.core.common.ui.VaultCardStyle
import com.aozijx.passly.core.common.ui.VaultCardStyle.DEFAULT
import com.aozijx.passly.core.common.ui.VaultCardStyle.PASSWORD
import com.aozijx.passly.core.common.ui.VaultCardStyle.TOTP

internal data class CardStyleGroupDefinition(
    val title: String, val styleCandidates: List<VaultCardStyle>
)

internal object CardStyleSettingsConfig {
    const val SECTION_TITLE = "列表卡片样式"
    const val SECTION_EXPANDED_HINT = "已展开预览"
    const val SECTION_COLLAPSED_HINT = "点击展开预览与切换"
    const val GROUP_EXPANDED_LABEL = "收起"
    const val GROUP_COLLAPSED_LABEL = "展开"

    val PASSWORD_GROUP = CardStyleGroupDefinition(
        title = "密码分组", styleCandidates = listOf(DEFAULT, PASSWORD)
    )

    val TOTP_GROUP = CardStyleGroupDefinition(
        title = "TOTP 分组", styleCandidates = listOf(DEFAULT, TOTP)
    )
}
