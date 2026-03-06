package com.example.poop.ui.screens.vault.components.items

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.poop.data.VaultEntry
import com.example.poop.ui.screens.vault.VaultViewModel
import com.example.poop.ui.screens.vault.components.common.VaultItemIcon
import com.example.poop.ui.screens.vault.utils.TwoFAUtils
import kotlinx.coroutines.delay

/**
 * 2FA 专用列表项：包含动态验证码显示
 */
@Composable
fun TwoFAItem(
    entry: VaultEntry,
    viewModel: VaultViewModel,
    showCode: Boolean = true
) {
    var totpCode by remember { mutableStateOf("------") }

    // 验证码刷新逻辑 (仅在有密钥且允许显示时运行)
    val secret = entry.totpSecret
    if (!secret.isNullOrEmpty() && showCode) {
        LaunchedEffect(secret, entry.totpAlgorithm, entry.totpDigits, entry.totpPeriod) {
            while (true) {
                // 列表页通常使用静默生成，此处直接使用存储的 secret (假设已解密或为简单演示)
                totpCode = TwoFAUtils.generateTotp(
                    secret = secret,
                    digits = if (entry.totpAlgorithm.uppercase() == "STEAM") 5 else entry.totpDigits,
                    period = entry.totpPeriod,
                    algorithm = entry.totpAlgorithm
                )
                delay(1000)
            }
        }
    }

    Card(
        onClick = { viewModel.showDetail(entry) },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            VaultItemIcon(item = entry)

            Spacer(modifier = Modifier.width(20.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    entry.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = entry.category,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (showCode && !secret.isNullOrEmpty()) {
                Text(
                    text = if (entry.totpAlgorithm.uppercase() == "STEAM") totpCode else totpCode.chunked(
                        3
                    ).joinToString(" "),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp
                    ),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
            Spacer(modifier = Modifier.width(20.dp))
        }
    }
}
