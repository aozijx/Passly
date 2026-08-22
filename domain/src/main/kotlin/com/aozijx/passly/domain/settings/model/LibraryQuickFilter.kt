package com.aozijx.passly.domain.settings.model

import com.aozijx.passly.domain.entry.model.query.EntryFilter

/**
 * 保险箱列表快捷筛选器。
 *
 * - [settingsKey] 用于持久化（不可改，改了会使历史偏好失效）。
 * - [isToggleable] 为 false 的筛选项始终显示，不可在设置中关闭（如 ALL）。
 * - [entryFilter] 对应到领域层的查询筛选条件。
 *
 * UI 标题、图标等展示信息必须放在 UI mapper 中，不能放进这个模型。
 */
enum class LibraryQuickFilter(
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
        /** 默认启用的快捷筛选集合：默认只保留必选项，筛选栏因此默认不显示。 */
        val defaultVisibleKeys: Set<String> =
            entries.filter { !it.isToggleable }
                .map { it.settingsKey }
                .toSet()

        /** 设置页可切换的快捷筛选项（已排除不再展示的选项）。 */
        val toggleableVisibleQuickFilters: List<LibraryQuickFilter> =
            entries.filter { it.isToggleable && it.isUiVisible }

        /** 根据偏好集合筛选出当前可见快捷筛选项，始终保留 [isToggleable] 为 false 的项。 */
        fun resolveVisible(enabledKeys: Set<String>): List<LibraryQuickFilter> =
            entries.filter {
                (!it.isToggleable || it.settingsKey in enabledKeys) &&
                        (it.isUiVisible || !it.isToggleable)
            }

        fun toggleVisibleKey(
            enabledKeys: Set<String>,
            quickFilter: LibraryQuickFilter
        ): Set<String> = buildSet {
            entries.filter { !it.isToggleable }
                .forEach { add(it.settingsKey) }
            toggleableVisibleQuickFilters.filter {
                it != quickFilter && it.settingsKey in enabledKeys
            }.forEach {
                add(it.settingsKey)
            }
            if (
                quickFilter.settingsKey !in enabledKeys &&
                quickFilter.isToggleable &&
                quickFilter.isUiVisible
            ) {
                add(quickFilter.settingsKey)
            }
        }
    }
}
