package com.aozijx.passly.feature.vault.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TextSnippet
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Pin
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.filled.Wifi
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
    PASSWORD(R.string.password, Icons.Default.Key),
    TOTP(R.string.otp, Icons.Default.Pin),
    BANK_CARD(R.string.vault_fab_bank_card, Icons.Default.CreditCard),
    WIFI(R.string.vault_fab_wifi, Icons.Default.Wifi),
    SSH_KEY(R.string.vault_fab_ssh_key, Icons.Default.VpnKey),
    ID_CARD(R.string.vault_fab_id_card, Icons.Default.Badge),
    SEED_PHRASE(R.string.vault_fab_seed_phrase, Icons.AutoMirrored.Filled.TextSnippet),
    PASSKEY(R.string.vault_fab_passkey, Icons.Default.Fingerprint),
    RECOVERY_CODE(R.string.vault_fab_recovery_code, Icons.Default.Restore);

    companion object {
        /** FAB 快捷菜单只保留直接创建的常用条目。 */
        val fabMenuOptions: List<AddType> = listOf(TOTP, PASSWORD)

        /** ModalBottomSheet 中显示的所有添加类型 */
        val allOptions: List<AddType> = entries.toList()
    }
}
