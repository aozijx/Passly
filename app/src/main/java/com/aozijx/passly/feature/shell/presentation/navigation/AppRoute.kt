package com.aozijx.passly.feature.shell.presentation.navigation

/**
 * Passly 全局导航路由表
 *
 * 结构：
 *  Vault    — 主保险箱列表（授权后的起始目的地）
 *  AddPassword — 新建密码条目的独立页面
 *  AddOtp / AddBankCard / AddWifi — 各类型新建条目的独立页面
 *  Detail   — 条目详情全屏页，携带 entryId 参数
 *  Settings — 设置页
 */
sealed class AppRoute(val route: String) {

    data object VaultGraph : AppRoute("vault_graph")

    data object Vault : AppRoute("vault")

    data object Trash : AppRoute("vault/trash")

    data object AddPassword : AppRoute("vault/add/password")

    data object AddOtp : AppRoute("vault/add/otp")

    data object AddBankCard : AppRoute("vault/add/bankcard")

    data object AddWifi : AppRoute("vault/add/wifi")

    data object EditPassword : AppRoute("vault/edit/password/{entryId}") {
        fun createRoute(entryId: String) = "vault/edit/password/$entryId"
    }

    data object EditOtp : AppRoute("vault/edit/otp/{entryId}") {
        fun createRoute(entryId: String) = "vault/edit/otp/$entryId"
    }

    data object EditBankCard : AppRoute("vault/edit/bankcard/{entryId}") {
        fun createRoute(entryId: String) = "vault/edit/bankcard/$entryId"
    }

    data object EditWifi : AppRoute("vault/edit/wifi/{entryId}") {
        fun createRoute(entryId: String) = "vault/edit/wifi/$entryId"
    }

    data object AddWifiFromSeed : AppRoute("vault/add/wifi/from-seed/{seedToken}") {
        const val ARG_SEED_TOKEN = "seedToken"
        fun createRoute(token: String) = "vault/add/wifi/from-seed/$token"
    }

    data object Settings : AppRoute("settings")

    data object Detail : AppRoute("detail/{entryId}") {
        const val ARG_ENTRY_ID = "entryId"
        fun createRoute(entryId: String) = "detail/$entryId"
    }
}
