package com.aozijx.passly.domain.config

import com.aozijx.passly.domain.model.VaultCardStyle

typealias AutofillUiMode = UserConfig.Vault.AutofillUiMode

data class UserConfig(
    val display: Display = Display(),
    val security: Security = Security(),
    val vault: Vault = Vault(),
    val backup: Backup = Backup(),
) {
    data class Security(
        val lockTimeout: Long = 60_000L,
        val isInvalidateKeyOnBioChange: Boolean = true,
        val isSecureContentEnabled: Boolean = true,
        val isPasswordPreferredAuthFirst: Boolean = true,
        val isDeviceCredentialFallbackEnabled: Boolean = true,
        val isFlipToLockEnabled: Boolean = false,
        val isFlipExitAndClearStackEnabled: Boolean = false,
    )

    data class Display(
        val isStatusBarAutoHide: Boolean = true,
        val isTopBarCollapsible: Boolean = true,
        val isTabBarCollapsible: Boolean = true,
        val isAutoDownloadIcons: Boolean = true,
        val cardStyle: VaultCardStyle = VaultCardStyle.DEFAULT,
        val perTypeMap: Map<Int, VaultCardStyle> = mapOf(-1 to VaultCardStyle.DEFAULT),
    )

    data class Vault(
        val autofillUiMode: AutofillUiMode = AutofillUiMode.SYSTEM_INLINE,
        val isSwipeEnabled: Boolean = true,
        val swipeLeftAction: SwipeActionType = SwipeActionType.COPY_PASSWORD,
        val swipeRightAction: SwipeActionType = SwipeActionType.DETAIL,
        val tabBarMaxTabsWithoutScroll: Int = 4,
        val visibleVaultTabs: Set<String>? = null,
    ) {
        enum class AutofillUiMode { SYSTEM_INLINE, BOTTOM_SHEET }
        enum class SwipeActionType { DELETE, DETAIL, COPY_PASSWORD, COPY_USERNAME, DISABLED }
    }

    data class Backup(
        val directoryUri: String? = null,
        val lastExportFileName: String? = null,
    )
}