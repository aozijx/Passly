package com.aozijx.passly.feature.vault.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Pin
import androidx.compose.ui.graphics.vector.ImageVector
import com.aozijx.passly.R
import com.aozijx.passly.domain.model.lookup.EntryFilter

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
    val isUiVisible: Boolean,
    val entryFilter: EntryFilter
) {
    ALL(
        settingsKey = "all",
        titleRes = R.string.vault_tab_all,
        icon = Icons.Default.Apps,
        isToggleable = true,
        isUiVisible = true,
        entryFilter = EntryFilter.ALL
    ),
    PASSWORDS(
        settingsKey = "passwords",
        titleRes = R.string.vault_tab_passwords,
        icon = Icons.Default.Key,
        isToggleable = true,
        isUiVisible = true,
        entryFilter = EntryFilter.PASSWORD_ONLY
    ),
    TOTP(
        settingsKey = "totp",
        titleRes = R.string.vault_tab_totp,
        icon = Icons.Default.Pin,
        isToggleable = true,
        isUiVisible = true,
        entryFilter = EntryFilter.TOTP_ONLY
    );

    companion object {
        /** 默认启用的 Tab 集合（仅包含仍需展示的 UI 选项 + 必选项）。 */
        val defaultVisibleKeys: Set<String> =
            entries.filter { !it.isToggleable }
                .map { it.settingsKey }
                .toSet()

        /** 设置页可切换的 Tab（已排除不再展示的选项）。 */
        val toggleableVisibleTabs: List<VaultTab> =
            entries.filter { it.isToggleable && it.isUiVisible }

        /** 根据偏好集合筛选出当前可见 Tab，始终保留 [isToggleable] 为 false 的 Tab。 */
        fun resolveVisible(enabledKeys: Set<String>): List<VaultTab> =
            entries.filter { (!it.isToggleable || it.settingsKey in enabledKeys) && (it.isUiVisible || !it.isToggleable) }
    }
}