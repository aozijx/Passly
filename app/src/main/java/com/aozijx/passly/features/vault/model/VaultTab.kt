package com.aozijx.passly.features.vault.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Pin
import androidx.compose.ui.graphics.vector.ImageVector
import com.aozijx.passly.R
import com.aozijx.passly.domain.repository.vault.VaultSearchRepository

/**
 * 保险箱列表 Tab 筛选器。
 *
 * - [settingsKey] 用于持久化（不可改，改了会使历史偏好失效）。
 * - [isToggleable] 为 false 的 Tab 始终显示，不可在设置中关闭（如 ALL）。
 * - [entryFilter] 对应到领域层的查询筛选条件。
 */
enum class VaultTab(
    val settingsKey: String,
    val titleRes: Int,
    val icon: ImageVector,
    val isToggleable: Boolean,
    val entryFilter: VaultSearchRepository.EntryFilter
) {
    ALL(
        settingsKey = "all",
        titleRes = R.string.vault_tab_all,
        icon = Icons.Default.Apps,
        isToggleable = false,
        entryFilter = VaultSearchRepository.EntryFilter.ALL
    ),
    PASSWORDS(
        settingsKey = "passwords",
        titleRes = R.string.vault_tab_passwords,
        icon = Icons.Default.Key,
        isToggleable = true,
        entryFilter = VaultSearchRepository.EntryFilter.PASSWORD_ONLY
    ),
    TOTP(
        settingsKey = "totp",
        titleRes = R.string.vault_tab_totp,
        icon = Icons.Default.Pin,
        isToggleable = true,
        entryFilter = VaultSearchRepository.EntryFilter.TOTP_ONLY
    );

    companion object {
        fun fromKey(key: String?): VaultTab? =
            entries.firstOrNull { it.settingsKey == key }

        /** 默认启用的 Tab 集合（全部）。 */
        val defaultVisibleKeys: Set<String> = entries.map { it.settingsKey }.toSet()

        /** 根据偏好集合筛选出当前可见 Tab，始终保留 [isToggleable] 为 false 的 Tab。 */
        fun resolveVisible(enabledKeys: Set<String>): List<VaultTab> =
            entries.filter { !it.isToggleable || it.settingsKey in enabledKeys }
    }
}