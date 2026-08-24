package com.aozijx.passly.presentation.ui.vault.list.component.fab

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
import com.aozijx.passly.presentation.ui.vault.list.model.VaultAddTypeUiModel

internal fun VaultAddTypeUiModel.icon(): ImageVector = when (this) {
    VaultAddTypeUiModel.PASSWORD -> Icons.Default.Key
    VaultAddTypeUiModel.TOTP -> Icons.Default.Pin
    VaultAddTypeUiModel.BANK_CARD -> Icons.Default.CreditCard
    VaultAddTypeUiModel.WIFI -> Icons.Default.Wifi
    VaultAddTypeUiModel.SSH_KEY -> Icons.Default.VpnKey
    VaultAddTypeUiModel.ID_CARD -> Icons.Default.Badge
    VaultAddTypeUiModel.SEED_PHRASE -> Icons.AutoMirrored.Filled.TextSnippet
    VaultAddTypeUiModel.PASSKEY -> Icons.Default.Fingerprint
    VaultAddTypeUiModel.RECOVERY_CODE -> Icons.Default.Restore
}
