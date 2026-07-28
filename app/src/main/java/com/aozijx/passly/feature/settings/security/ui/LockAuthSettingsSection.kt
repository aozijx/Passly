package com.aozijx.passly.feature.settings.security.ui

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
import com.aozijx.passly.core.ui.components.group.RoundedGroupItem
import com.aozijx.passly.core.ui.components.group.navigationSettingsGroupItem
import com.aozijx.passly.core.ui.components.group.switchSettingsGroupItem
import com.aozijx.passly.core.ui.components.settings.SettingsSectionTitle
import com.aozijx.passly.domain.settings.model.LockTimeoutConstraints
import kotlin.math.roundToInt

private const val SLIDER_MIN_SECONDS = (LockTimeoutConstraints.SLIDER_MIN_MS / 1000L).toFloat()
private const val SLIDER_MAX_SECONDS = (LockTimeoutConstraints.MAX_MS / 1000L).toFloat()
private const val SLIDER_STEP_SECONDS = (LockTimeoutConstraints.SLIDER_STEP_MS / 1000L).toFloat()

@Composable
fun LockAuthSettingsSection(
    lockTimeout: Long,
    isAppPasswordEnabled: Boolean,
    isBiometricEnabled: Boolean,
    isInvalidateKeyOnBioChange: Boolean,
    isLockOnBackground: Boolean,
    onLockTimeoutChange: (Long) -> Unit,
    onAppPasswordClick: () -> Unit,
    onBiometricEnabledChange: (Boolean) -> Unit,
    onInvalidateKeyOnBioChangeToggle: (Boolean) -> Unit,
    onLockOnBackgroundChange: (Boolean) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val currentSeconds = (lockTimeout / 1000L).toFloat()
        .coerceIn(SLIDER_MIN_SECONDS, SLIDER_MAX_SECONDS)
    var sliderValue by remember(lockTimeout) { mutableFloatStateOf(currentSeconds) }

    SettingsSectionTitle(text = stringResource(R.string.security_auth_section))
    RoundedGroup(
        items = listOf(

            switchSettingsGroupItem(
                key = "security.lock_on_background",
                icon = Icons.Default.Lock,
                title = stringResource(R.string.security_lock_on_background),
                subtitle = stringResource(R.string.security_lock_on_background_description),
                checked = isLockOnBackground,
                onCheckedChange = onLockOnBackgroundChange
            ),
            navigationSettingsGroupItem(
                key = "security.lock_timeout",
                icon = Icons.Default.Timer,
                title = stringResource(R.string.security_auto_lock),
                value = formatLockTimeoutText(lockTimeout),
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
                            text = stringResource(R.string.security_auto_lock_delay),
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
                                ((sliderValue / SLIDER_STEP_SECONDS).roundToInt() * SLIDER_STEP_SECONDS)
                                    .coerceIn(SLIDER_MIN_SECONDS, SLIDER_MAX_SECONDS)
                            sliderValue = rounded
                            onLockTimeoutChange(rounded.toLong() * 1000L)
                        },
                        valueRange = SLIDER_MIN_SECONDS..SLIDER_MAX_SECONDS,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = stringResource(
                                R.string.duration_seconds,
                                SLIDER_MIN_SECONDS.roundToInt()
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Text(
                            text = stringResource(
                                R.string.duration_minutes,
                                (SLIDER_MAX_SECONDS / 60f).roundToInt()
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
                title = stringResource(R.string.security_app_password),
                subtitle = stringResource(R.string.security_app_password_description),
                value = stringResource(
                    if (isAppPasswordEnabled) R.string.configured else R.string.not_set
                ),
                onClick = onAppPasswordClick
            ),
            switchSettingsGroupItem(
                key = "security.biometric_enabled",
                icon = Icons.Default.Fingerprint,
                title = stringResource(R.string.security_biometric_unlock),
                subtitle = stringResource(
                    if (isBiometricEnabled) {
                        R.string.security_biometric_enabled_description
                    } else {
                        R.string.security_biometric_disabled_description
                    }
                ),
                checked = isBiometricEnabled,
                onCheckedChange = onBiometricEnabledChange
            ),
            switchSettingsGroupItem(
                key = "security.invalidate_biometric",
                visible = isBiometricEnabled,
                icon = Icons.Default.Fingerprint,
                title = stringResource(R.string.security_invalidate_biometric_key),
                subtitle = stringResource(
                    if (isInvalidateKeyOnBioChange) {
                        R.string.security_invalidate_biometric_key_enabled
                    } else {
                        R.string.security_invalidate_biometric_key_disabled
                    }
                ),
                checked = isInvalidateKeyOnBioChange,
                onCheckedChange = onInvalidateKeyOnBioChangeToggle
            )
        )
    )
}

@Composable
private fun formatLockTimeoutText(timeoutMs: Long): String {
    val seconds = (timeoutMs / 1000L).coerceAtLeast(1L)
    return when {
        seconds < 60L -> stringResource(R.string.duration_seconds, seconds)
        seconds % 60L == 0L ->
            stringResource(R.string.duration_minutes, seconds / 60L)

        else -> stringResource(
            R.string.duration_minutes_seconds,
            seconds / 60L,
            seconds % 60L
        )
    }
}
