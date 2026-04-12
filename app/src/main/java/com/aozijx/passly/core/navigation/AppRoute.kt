package com.aozijx.passly.core.navigation

/**
 * Passly 全局导航路由表
 *
 * 结构：
 *  Vault    — 主保险箱列表（授权后的起始目的地）
 *  Detail   — 条目详情全屏页，携带 entryId 参数
 *  Settings — 设置页
 */
sealed class AppRoute(val route: String) {

    data object Vault : AppRoute("vault")

    data object Settings : AppRoute("settings")

    data object Detail : AppRoute("detail/{entryId}") {
        const val ARG_ENTRY_ID = "entryId"
        fun createRoute(entryId: Int) = "detail/$entryId"
    }
}