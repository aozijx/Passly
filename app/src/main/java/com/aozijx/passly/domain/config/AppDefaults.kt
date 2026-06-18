package com.aozijx.passly.domain.config

import com.aozijx.passly.R
import com.aozijx.passly.domain.model.EntryType
import com.aozijx.passly.domain.model.VaultCardStyle

object AppDefaults {
    const val MIN_LOCK_TIMEOUT_MS = 10_000L
    const val MAX_LOCK_TIMEOUT_MS = 300_000L
    const val LOCK_TIMEOUT_SLIDER_STEP_MS = 5_000L
    const val SLIDER_MIN_LOCK_TIMEOUT_MS = 15_000L

    const val TAB_THRESHOLD_MIN = 2
    const val TAB_THRESHOLD_MAX = 8

    object CardStyle {
        val GLOBAL_DEFAULT_STYLE: VaultCardStyle = VaultCardStyle.DEFAULT

        val SETTINGS_STYLES: List<VaultCardStyle> = listOf(
            VaultCardStyle.DEFAULT, VaultCardStyle.PASSWORD, VaultCardStyle.TOTP
        )
        val PER_TYPE_STYLES: List<VaultCardStyle> = listOf(
            VaultCardStyle.DEFAULT, VaultCardStyle.PASSWORD, VaultCardStyle.TOTP
        )

        data class TypeStylePolicy(
            val defaultStyle: VaultCardStyle,
            val selectableStyles: List<VaultCardStyle>
        )

        val TYPE_STYLE_POLICY_MAP: Map<EntryType, TypeStylePolicy> =
            EntryType.entries.associateWith {
                TypeStylePolicy(
                    defaultStyle = VaultCardStyle.DEFAULT,
                    selectableStyles = listOf(VaultCardStyle.DEFAULT, VaultCardStyle.PASSWORD)
                )
            } + mapOf(
                EntryType.TOTP to TypeStylePolicy(
                    defaultStyle = VaultCardStyle.DEFAULT,
                    selectableStyles = listOf(VaultCardStyle.DEFAULT, VaultCardStyle.TOTP)
                )
            )

        data class SettingsGroupSpec(
            @field:androidx.annotation.StringRes val titleRes: Int,
            val entryType: EntryType,
            val styleCandidates: List<VaultCardStyle>
        ) {
            val entryTypeValue: Int get() = entryType.value
        }

        private val SETTINGS_GROUP_TITLE_BY_TYPE: Map<EntryType, Int> = mapOf(
            EntryType.PASSWORD to R.string.settings_card_style_group_password,
            EntryType.TOTP to R.string.settings_card_style_group_totp
        )

        val SETTINGS_GROUP_SPECS: List<SettingsGroupSpec> =
            SETTINGS_GROUP_TITLE_BY_TYPE.map { (entryType, titleRes) ->
                SettingsGroupSpec(
                    titleRes = titleRes,
                    entryType = entryType,
                    styleCandidates = policyFor(entryType).selectableStyles
                )
            }

        fun policyFor(entryType: EntryType): TypeStylePolicy =
            TYPE_STYLE_POLICY_MAP.getValue(entryType)

        fun normalizeGlobalStyle(style: VaultCardStyle): VaultCardStyle =
            if (style in SETTINGS_STYLES) style else GLOBAL_DEFAULT_STYLE
    }
}