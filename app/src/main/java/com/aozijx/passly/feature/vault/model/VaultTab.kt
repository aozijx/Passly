package com.aozijx.passly.feature.vault.model

import com.aozijx.passly.domain.entry.model.lookup.EntryFilter

/**
 * 保险箱列表分类筛选器。
 *
 * - [settingsKey] 用于持久化（不可改，改了会使历史偏好失效）。
 * - [isToggleable] 为 false 的分类始终显示，不可在设置中关闭（如 ALL）。
 * - [entryFilter] 对应到领域层的查询筛选条件。
 *
 * UI 标题、图标等展示信息必须放在 UI mapper 中，不能放进这个模型。
 */
enum class VaultTab(
    val settingsKey: String,
    val isToggleable: Boolean,
    val isUiVisible: Boolean,
    val entryFilter: EntryFilter
) {
    ALL(
        settingsKey = "all",
        isToggleable = false,
        isUiVisible = true,
        entryFilter = EntryFilter.ALL
    ),
    PASSWORDS(
        settingsKey = "passwords",
        isToggleable = true,
        isUiVisible = true,
        entryFilter = EntryFilter.PASSWORD_ONLY
    ),
    TOTP(
        settingsKey = "totp",
        isToggleable = true,
        isUiVisible = true,
        entryFilter = EntryFilter.TOTP_ONLY
    );

    companion object {
        /** 默认启用的 Tab 集合：默认只保留必选项，分类栏因此默认不显示。 */
        val defaultVisibleKeys: Set<String> =
            entries.filter { !it.isToggleable }
                .map { it.settingsKey }
                .toSet()

        /** 设置页可切换的 Tab（已排除不再展示的选项）。 */
        val toggleableVisibleTabs: List<VaultTab> =
            entries.filter { it.isToggleable && it.isUiVisible }

        /** 根据偏好集合筛选出当前可见分类，始终保留 [isToggleable] 为 false 的分类。 */
        fun resolveVisible(enabledKeys: Set<String>): List<VaultTab> =
            entries.filter {
                (!it.isToggleable || it.settingsKey in enabledKeys) &&
                        (it.isUiVisible || !it.isToggleable)
            }

        fun toggleVisibleKey(
            enabledKeys: Set<String>,
            tab: VaultTab
        ): Set<String> = buildSet {
            entries.filter { !it.isToggleable }
                .forEach { add(it.settingsKey) }
            toggleableVisibleTabs.filter {
                it != tab && it.settingsKey in enabledKeys
            }.forEach {
                add(it.settingsKey)
            }
            if (tab.settingsKey !in enabledKeys && tab.isToggleable && tab.isUiVisible) {
                add(tab.settingsKey)
            }
        }
    }
}
