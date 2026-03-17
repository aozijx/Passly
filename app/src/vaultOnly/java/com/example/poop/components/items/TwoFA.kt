package com.example.poop.components.items

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.example.poop.MainViewModel
import com.example.poop.R
import com.example.poop.common.icons.VaultItemIcon
import com.example.poop.data.VaultEntry

/**
 * 2FA 专用列表项：包含动态验证码显示
 */
@Composable
fun TwoFAItem(
    entry: VaultEntry,
    viewModel: MainViewModel,
    showCode: Boolean = true
) {
    val context = LocalContext.current
    // 从 ViewModel 订阅全局 TOTP 状态
    val totpStates by viewModel.totpStates.collectAsState()
    val currentState = totpStates[entry.id]
    
    val isSteam = remember(entry.totpAlgorithm) { entry.totpAlgorithm.uppercase() == "STEAM" }

    Card(
        onClick = { 
            if (currentState?.decryptedSecret == null) {
                // 如果未解密，调用 ViewModel 提供的统一解锁入口
                viewModel.ensureTotpUnlocked(context as FragmentActivity, entry)
            } else {
                // 已解密状态下点击进入详情
                viewModel.showDetail(entry)
            }
        },
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
            
            // 进度条跟随全局状态刷新
            CircularProgressIndicator(
                progress = { currentState?.progress ?: 0f },
                modifier = Modifier.size(24.dp),
                strokeWidth = 3.dp,
                color = if ((currentState?.progress ?: 1f) < 0.2f) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            )

            if (showCode) {
                Text(
                    text = if (currentState?.decryptedSecret == null) {
                        stringResource(R.string.auth_to_show)
                    } else {
                        if (isSteam) currentState.code else currentState.code.chunked(3).joinToString(" ")
                    },
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp,
                        fontSize = if (currentState?.decryptedSecret == null) 14.sp else 18.sp
                    ),
                    color = if (currentState?.decryptedSecret == null) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 8.dp)
                )

                Spacer(modifier = Modifier.width(16.dp))
            }
        }
    }
}
