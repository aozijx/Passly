package com.aozijx.passly.features.auth.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.aozijx.passly.R
import com.aozijx.passly.features.auth.AuthCoordinator

/**
 * 授权验证屏幕：系统自动选择可用生物识别方式，提供重试入口。
 */
@Composable
fun AuthScreen(
    authCoordinator: AuthCoordinator, activity: FragmentActivity
) {
    var authInProgress by remember { mutableStateOf(false) }
    val title = stringResource(R.string.vault_auth_decrypt_title)
    val subtitle = stringResource(R.string.vault_auth_subtitle)

    // 触发系统生物识别逻辑
    fun requestAuth() {
        if (authInProgress) return
        authInProgress = true
        authCoordinator.authenticate(
            activity = activity,
            title = title,
            subtitle = subtitle,
            onSuccess = { authInProgress = false },
            onError = { authInProgress = false })
    }

    LaunchedEffect(Unit) {
        requestAuth()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // 顶部：安全状态标识
        Box(
            modifier = Modifier
                .size(80.dp)
                .background(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                    shape = CircleShape
                ), contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                modifier = Modifier.size(36.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = stringResource(R.string.vault_locked_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(64.dp))

        FilledTonalButton(
            onClick = { requestAuth() },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = !authInProgress,
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                text = if (authInProgress) "验证中..." else stringResource(R.string.auth_verify_now),
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}