package com.example.poop.util

import android.content.Context
import android.content.Intent
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.example.poop.R
import com.example.poop.ui.screens.scanner.ScannerActivity
import com.example.poop.ui.screens.vault.VaultActivity

object ShortcutManager {

    fun init(context: Context) {
        // 创建“打开保险库”快捷方式
        val vaultShortcut = ShortcutInfoCompat.Builder(context, "open_vault")
            .setShortLabel(context.getString(R.string.shortcut_vault_short))
            .setLongLabel(context.getString(R.string.shortcut_vault_long))
            .setIcon(IconCompat.createWithResource(context, R.drawable.outline_lock_person_24))
            .setIntent(
                Intent(context, VaultActivity::class.java).apply {
                    action = Intent.ACTION_VIEW
                }
            )
            .build()

        // 创建“扫一扫”快捷方式
        val scanShortcut = ShortcutInfoCompat.Builder(context, "scan_qr")
            .setShortLabel(context.getString(R.string.shortcut_scanner_short))
            .setLongLabel(context.getString(R.string.shortcut_scanner_long))
            .setIcon(IconCompat.createWithResource(context, R.drawable.baseline_qr_code_scanner_24))
            .setIntent(
                Intent(context, ScannerActivity::class.java).apply {
                    action = Intent.ACTION_VIEW
                }
            )
            .build()

        // 更新动态快捷方式
        ShortcutManagerCompat.addDynamicShortcuts(context, listOf(vaultShortcut, scanShortcut))
    }
}
