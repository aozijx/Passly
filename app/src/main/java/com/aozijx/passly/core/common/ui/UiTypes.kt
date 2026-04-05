package com.aozijx.passly.core.common.ui

import androidx.annotation.StringRes
import com.aozijx.passly.R
import com.aozijx.passly.core.common.EntryType

/**
 * 列表卡片样式
 */
enum class VaultCardStyle(
    val key: String,
    @field:StringRes val displayNameRes: Int,
    @field:StringRes val descriptionRes: Int
) {
    DEFAULT("default", R.string.settings_card_style_default_name, R.string.settings_card_style_default_desc),
    PASSWORD("password", R.string.settings_card_style_password_name, R.string.settings_card_style_password_desc),
    TOTP("totp", R.string.settings_card_style_totp_name, R.string.settings_card_style_totp_desc);

    companion object {
        val settingsStyles: List<VaultCardStyle> = listOf(PASSWORD, TOTP)
        val perTypeStyles: List<VaultCardStyle> = listOf(DEFAULT, PASSWORD, TOTP)
        val globalDefaultStyle: VaultCardStyle = PASSWORD

        data class TypeStylePolicy(
            val defaultStyle: VaultCardStyle,
            val selectableStyles: List<VaultCardStyle>
        )

        data class SettingsGroupSpec(
            @field:StringRes val titleRes: Int,
            val entryType: EntryType,
            val styleCandidates: List<VaultCardStyle>
        ) {
            val entryTypeValue: Int get() = entryType.value
        }

        // 数据结构驱动的分类样式策略。
        private val typeStylePolicyMap: Map<EntryType, TypeStylePolicy> =
            EntryType.entries.associateWith {
                TypeStylePolicy(
                    defaultStyle = PASSWORD,
                    selectableStyles = listOf(DEFAULT, PASSWORD)
                )
            } + mapOf(
                EntryType.TOTP to TypeStylePolicy(
                    defaultStyle = DEFAULT,
                    selectableStyles = listOf(DEFAULT, TOTP)
                )
            )

        private val settingsGroupTitleByType: Map<EntryType, Int> = mapOf(
            EntryType.PASSWORD to R.string.settings_card_style_group_password,
            EntryType.TOTP to R.string.settings_card_style_group_totp
        )

        val settingsGroupSpecs: List<SettingsGroupSpec> = settingsGroupTitleByType.map { (entryType, titleRes) ->
            SettingsGroupSpec(
                titleRes = titleRes,
                entryType = entryType,
                styleCandidates = policyFor(entryType).selectableStyles
            )
        }

        fun fromKey(key: String?): VaultCardStyle {
            val normalizedKey = key?.trim()?.lowercase()
            return entries.firstOrNull { it.key == normalizedKey } ?: DEFAULT
        }

        fun policyFor(entryType: EntryType): TypeStylePolicy {
            return typeStylePolicyMap.getValue(entryType)
        }

        fun normalizeGlobalStyle(style: VaultCardStyle): VaultCardStyle {
            return if (style in settingsStyles) style else globalDefaultStyle
        }

        fun resolveForEntryType(selectedStyle: VaultCardStyle, entryTypeValue: Int): VaultCardStyle {
            val policy = policyFor(EntryType.fromValue(entryTypeValue))
            val normalizedSelectedStyle = if (selectedStyle in policy.selectableStyles) {
                selectedStyle
            } else {
                DEFAULT
            }
            return if (normalizedSelectedStyle == DEFAULT) policy.defaultStyle else normalizedSelectedStyle
        }
    }
}
