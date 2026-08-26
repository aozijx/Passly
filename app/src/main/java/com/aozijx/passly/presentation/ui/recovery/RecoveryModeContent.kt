package com.aozijx.passly.presentation.ui.recovery

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aozijx.passly.R
import com.aozijx.passly.core.ui.components.settings.SettingsSection
import com.aozijx.passly.core.ui.components.settings.SettingsSectionTitle

@Composable
fun RecoveryModeContent(isSettingPassword: Boolean, onSetPassword: () -> Unit, onExit: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().navigationBarsPadding().verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(Icons.Default.Restore, null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(16.dp))
        Text(stringResource(R.string.recovery_mode_title), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(stringResource(R.string.recovery_mode_description), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(32.dp))
        SettingsSection(Modifier.fillMaxWidth()) {
            SettingsSectionTitle(stringResource(R.string.authentication_label))
            RecoveryActionCard(Icons.Default.LockReset, stringResource(R.string.recovery_mode_set_password), stringResource(R.string.app_password_unlock_description), onSetPassword, isSettingPassword)
        }
        Spacer(Modifier.height(16.dp))
        SettingsSection(Modifier.fillMaxWidth()) {
            RecoveryActionCard(Icons.AutoMirrored.Filled.Logout, stringResource(R.string.recovery_mode_exit), stringResource(R.string.recovery_mode_locked_description), onExit, containerColor = MaterialTheme.colorScheme.surfaceContainerHighest)
        }
    }
}

@Composable
private fun RecoveryActionCard(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit, isLoading: Boolean = false, containerColor: Color = MaterialTheme.colorScheme.surfaceContainerHigh) {
    Surface(onClick = onClick, enabled = !isLoading, modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large, color = containerColor) {
        Row(Modifier.padding(horizontal = 20.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (isLoading) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
        }
    }
}
