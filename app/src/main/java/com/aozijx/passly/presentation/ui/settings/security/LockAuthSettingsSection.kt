package com.aozijx.passly.presentation.ui.settings.security

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aozijx.passly.R
import com.aozijx.passly.core.ui.components.group.GroupCard
import com.aozijx.passly.core.ui.components.group.RoundedGroup
import com.aozijx.passly.core.ui.components.group.model.RoundedGroupItem
import com.aozijx.passly.core.ui.components.group.navigationSettingsGroupItem
import com.aozijx.passly.core.ui.components.group.switchSettingsGroupItem
import com.aozijx.passly.core.ui.components.settings.SettingsSectionTitle
import com.aozijx.passly.presentation.ui.settings.security.model.SecuritySettingsUiModel
import kotlin.math.roundToInt

@Composable
fun LockAuthSettingsSection(
    state: SecuritySettingsUiModel,
    onLockTimeoutChange: (Long) -> Unit,
    onAppPasswordClick: () -> Unit,
    onBiometricEnabledChange: (Boolean) -> Unit,
    onInvalidateKeyOnBioChangeToggle: (Boolean) -> Unit,
    onLockOnBackgroundChange: (Boolean) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val currentSeconds = (state.lockTimeoutMs / 1000L).toFloat()
        .coerceIn(state.sliderMinSeconds, state.sliderMaxSeconds)
    var sliderValue by remember(state.lockTimeoutMs) { mutableFloatStateOf(currentSeconds) }

    SettingsSectionTitle(text = stringResource(R.string.authentication_label))
    RoundedGroup(
        items = listOf(

            switchSettingsGroupItem(
                key = "security.lock_on_background",
                icon = Icons.Default.Lock,
                title = stringResource(R.string.settings_security_lock_on_background),
                subtitle = stringResource(R.string.settings_security_lock_on_background_description),
                checked = state.isLockOnBackground,
                onCheckedChange = onLockOnBackgroundChange
            ),
            navigationSettingsGroupItem(
                key = "security.lock_timeout",
                icon = Icons.Default.Timer,
                title = stringResource(R.string.settings_security_auto_lock),
                value = formatLockTimeoutText(state.lockTimeoutMs),
                onClick = { expanded = !expanded }
            ),
            RoundedGroupItem(
                key = "security.lock_timeout_slider",
                visible = expanded
            ) { itemScope ->
                GroupCard(itemScope = itemScope, contentPadding = PaddingValues(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.settings_security_auto_lock_delay),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = formatLockTimeoutText((sliderValue.toLong() * 1000L)),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Slider(
                        value = sliderValue,
                        onValueChange = { sliderValue = it },
                        onValueChangeFinished = {
                            val rounded =
                                ((sliderValue / state.sliderStepSeconds).roundToInt() *
                                    state.sliderStepSeconds)
                                    .coerceIn(state.sliderMinSeconds, state.sliderMaxSeconds)
                            sliderValue = rounded
                            onLockTimeoutChange(rounded.toLong() * 1000L)
                        },
                        valueRange = state.sliderMinSeconds..state.sliderMaxSeconds,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = stringResource(
                                R.string.settings_duration_seconds,
                                state.sliderMinSeconds.roundToInt()
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Text(
                            text = stringResource(
                                R.string.settings_duration_minutes,
                                (state.sliderMaxSeconds / 60f).roundToInt()
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            },
            navigationSettingsGroupItem(
                key = "security.app_password",
                icon = Icons.Default.Lock,
                title = stringResource(R.string.settings_security_app_password),
                subtitle = stringResource(R.string.app_password_unlock_description),
                value = stringResource(
                    if (state.isAppPasswordEnabled) R.string.settings_value_configured else R.string.not_set
                ),
                onClick = onAppPasswordClick
            ),
            switchSettingsGroupItem(
                key = "security.biometric_enabled",
                icon = Icons.Default.Fingerprint,
                title = stringResource(R.string.settings_security_biometric_unlock),
                subtitle = stringResource(
                    if (state.isBiometricEnabled) {
                        R.string.settings_security_biometric_enabled_description
                    } else {
                        R.string.settings_security_biometric_disabled_description
                    }
                ),
                checked = state.isBiometricEnabled,
                onCheckedChange = onBiometricEnabledChange
            ),
            switchSettingsGroupItem(
                key = "security.invalidate_biometric",
                visible = state.isBiometricEnabled,
                icon = Icons.Default.Fingerprint,
                title = stringResource(R.string.settings_security_invalidate_biometric_key),
                subtitle = stringResource(
                    if (state.isInvalidateKeyOnBioChange) {
                        R.string.settings_security_invalidate_biometric_key_enabled
                    } else {
                        R.string.settings_security_invalidate_biometric_key_disabled
                    }
                ),
                checked = state.isInvalidateKeyOnBioChange,
                onCheckedChange = onInvalidateKeyOnBioChangeToggle
            )
        )
    )
}

@Composable
private fun formatLockTimeoutText(timeoutMs: Long): String {
    val seconds = (timeoutMs / 1000L).coerceAtLeast(1L)
    return when {
        seconds < 60L -> stringResource(R.string.settings_duration_seconds, seconds)
        seconds % 60L == 0L ->
            stringResource(R.string.settings_duration_minutes, seconds / 60L)

        else -> stringResource(
            R.string.settings_duration_minutes_seconds,
            seconds / 60L,
            seconds % 60L
        )
    }
}
