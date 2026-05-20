package com.aozijx.passly.domain.config

import com.aozijx.passly.R
import com.aozijx.passly.core.common.AutofillUiMode
import com.aozijx.passly.core.common.EntryType
import com.aozijx.passly.core.common.SwipeActionType
import com.aozijx.passly.domain.model.VaultCardStyle

object AppDefaults {

    object Security {
        const val DEFAULT_LOCK_TIMEOUT_MS = 60_000L
        const val MIN_LOCK_TIMEOUT_MS = 5_000L

        val LOCK_TIMEOUT_PRESETS = listOf(
            15_000L to "15 秒",
            30_000L to "30 秒",
            60_000L to "1 分钟",
            120_000L to "2 分钟",
            300_000L to "5 分钟",
            600_000L to "10 分钟"
        )

        const val DEFAULT_INVALIDATE_KEY_ON_BIO_CHANGE = true
        const val DEFAULT_SECURE_CONTENT_ENABLED = true
        const val DEFAULT_PASSWORD_PREFERRED_AUTH_FIRST = true
        const val DEFAULT_DEVICE_CREDENTIAL_FALLBACK = true
        const val DEFAULT_FLIP_TO_LOCK = false
        const val DEFAULT_FLIP_EXIT_AND_CLEAR_STACK = false
    }

    object Display {
        const val DEFAULT_DYNAMIC_COLOR = true
        const val DEFAULT_STATUS_BAR_AUTO_HIDE = true
        const val DEFAULT_TOP_BAR_COLLAPSIBLE = true
        const val DEFAULT_TAB_BAR_COLLAPSIBLE = true
        const val DEFAULT_AUTO_DOWNLOAD_ICONS = true
    }

    object Vault {
        const val DEFAULT_SWIPE_ENABLED = true
        val DEFAULT_SWIPE_LEFT_ACTION: SwipeActionType = SwipeActionType.COPY_PASSWORD
        val DEFAULT_SWIPE_RIGHT_ACTION: SwipeActionType = SwipeActionType.DETAIL
        val DEFAULT_AUTOFILL_UI_MODE: AutofillUiMode = AutofillUiMode.SYSTEM_INLINE
        const val DEFAULT_TAB_BAR_MAX_TABS = 4
        const val TAB_THRESHOLD_MIN = 2
        const val TAB_THRESHOLD_MAX = 8
    }

    object CardStyle {
        val GLOBAL_DEFAULT_STYLE: VaultCardStyle = VaultCardStyle.DEFAULT
        val GLOBAL_DEFAULT_STYLE_KEY = -1
        val GLOBAL_DEFAULT_STYLE_ENTRY: Pair<Int, VaultCardStyle> =
            GLOBAL_DEFAULT_STYLE_KEY to GLOBAL_DEFAULT_STYLE

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