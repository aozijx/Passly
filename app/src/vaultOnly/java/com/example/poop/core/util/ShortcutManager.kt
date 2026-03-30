package com.example.poop.core.util

import android.content.Context
import android.content.Intent
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.example.poop.R
import com.example.poop.ui.navigation.Screen

object ShortcutManager {

    private const val VAULT_ACTIVITY_CLASS = "com.example.poop.ui.screens.vault.VaultActivity"

    fun init(context: Context) {
        // 如果当前构建版本不支持 Vault 功能，则不添加快捷方式
        if (!Screen.Companion.isVaultAvailable()) {
            ShortcutManagerCompat.removeAllDynamicShortcuts(context)
            return
        }

        // 创建“打开保险库”快捷方式
        val vaultShortcut = ShortcutInfoCompat.Builder(context, "open_vault")
            .setShortLabel(context.getString(R.string.shortcut_vault_short))
            .setLongLabel(context.getString(R.string.shortcut_vault_long))
            .setIcon(IconCompat.createWithResource(context, R.drawable.outline_lock_person_24))
            .setIntent(
                Intent().apply {
                    setClassName(context.packageName, VAULT_ACTIVITY_CLASS)
                    action = Intent.ACTION_VIEW
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
            )
            .build()

        // 创建“扫一扫”快捷方式
        val scanShortcut = ShortcutInfoCompat.Builder(context, "scan_qr")
            .setShortLabel(context.getString(R.string.shortcut_scanner_short))
            .setLongLabel(context.getString(R.string.shortcut_scanner_long))
            .setIcon(IconCompat.createWithResource(context, R.drawable.baseline_qr_code_scanner_24))
            .setIntent(
                Intent().apply {
                    setClassName(context.packageName, VAULT_ACTIVITY_CLASS)
                    action = "ACTION_SCAN_QR"
                    putExtra("START_SCAN", true)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
            )
            .build()

        // 更新动态快捷方式
        ShortcutManagerCompat.addDynamicShortcuts(context, listOf(vaultShortcut, scanShortcut))
    }
}