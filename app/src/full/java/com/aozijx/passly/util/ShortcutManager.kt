package com.aozijx.passly.util

import android.content.Context
import android.content.Intent
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.aozijx.passly.BuildConfig
import com.aozijx.passly.R

object ShortcutManager {
    fun init(context: Context) {
        // 创建“打开保险库”快捷方式
        val vaultShortcut = ShortcutInfoCompat.Builder(context, "open_vault")
            .setShortLabel(context.getString(R.string.shortcut_vault_short))
            .setLongLabel(context.getString(R.string.shortcut_vault_long))
            .setIcon(IconCompat.createWithResource(context, R.drawable.outline_lock_person_24))
            .setIntent(
                Intent().apply {
                    setClassName(context.packageName, BuildConfig.VAULT_ACTIVITY_CLASS)
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
                    setClassName(context.packageName, BuildConfig.VAULT_ACTIVITY_CLASS)
                    action = "ACTION_SCAN_QR"
                    putExtra("START_SCAN", true)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
            )
            .build()

        // 设置动态快捷方式
        ShortcutManagerCompat.setDynamicShortcuts(context, listOf(vaultShortcut, scanShortcut))
    }
}
