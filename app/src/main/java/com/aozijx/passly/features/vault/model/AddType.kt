package com.aozijx.passly.features.vault.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Pin
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.ui.graphics.vector.ImageVector
import com.aozijx.passly.R

/**
 * 保险箱新增操作类型。
 *
 * 无操作状态用可空类型 `AddType?` 表达（`null` 即未触发任何新增动作）。
 */
enum class AddType(
    val labelRes: Int,
    val icon: ImageVector
) {
    PASSWORD(R.string.vault_fab_password, Icons.Default.Key),
    TOTP(R.string.vault_fab_2fa, Icons.Default.Pin),
    SCAN(R.string.vault_fab_scan, Icons.Default.QrCodeScanner)
}