package com.aozijx.passly.presentation.feature.vault.list.component.fab

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
import com.aozijx.passly.feature.vault.model.AddType

internal fun AddType.icon(): ImageVector = when (this) {
    AddType.PASSWORD -> Icons.Default.Key
    AddType.TOTP -> Icons.Default.Pin
    AddType.BANK_CARD -> Icons.Default.CreditCard
    AddType.WIFI -> Icons.Default.Wifi
    AddType.SSH_KEY -> Icons.Default.VpnKey
    AddType.ID_CARD -> Icons.Default.Badge
    AddType.SEED_PHRASE -> Icons.AutoMirrored.Filled.TextSnippet
    AddType.PASSKEY -> Icons.Default.Fingerprint
    AddType.RECOVERY_CODE -> Icons.Default.Restore
}
