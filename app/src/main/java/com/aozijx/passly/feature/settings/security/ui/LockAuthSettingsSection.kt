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
import androidx.compose.ui.unit.dp
import com.aozijx.passly.core.ui.components.group.GroupCard
import com.aozijx.passly.core.ui.components.group.RoundedGroup
import com.aozijx.passly.core.ui.components.group.RoundedGroupItem
import com.aozijx.passly.core.ui.components.group.navigationSettingsGroupItem
import com.aozijx.passly.core.ui.components.group.switchSettingsGroupItem
import com.aozijx.passly.core.ui.components.settings.SettingsSectionTitle
import com.aozijx.passly.domain.settings.model.LockTimeoutConstraints
import com.aozijx.passly.feature.settings.security.formatLockTimeoutText
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

    SettingsSectionTitle(text = "认证")
    RoundedGroup(
        items = listOf(

            switchSettingsGroupItem(
                key = "security.lock_on_background",
                icon = Icons.Default.Lock,
                title = "立即锁定",
                subtitle = "退出 app 后锁定",
                checked = isLockOnBackground,
                onCheckedChange = onLockOnBackgroundChange
            ),
            navigationSettingsGroupItem(
                key = "security.lock_timeout",
                icon = Icons.Default.Timer,
                title = "自动锁定",
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
                            text = "自动锁定时间",
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
                            text = "${SLIDER_MIN_SECONDS.roundToInt()} 秒",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Text(
                            text = "${(SLIDER_MAX_SECONDS / 60f).roundToInt()} 分钟",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            },
            navigationSettingsGroupItem(
                key = "security.app_password",
                icon = Icons.Default.Lock,
                title = "设置密码",
                subtitle = "独立于系统锁屏，用密码直接解锁应用",
                value = if (isAppPasswordEnabled) "已设置" else "未设置",
                onClick = onAppPasswordClick
            ),
            switchSettingsGroupItem(
                key = "security.biometric_enabled",
                icon = Icons.Default.Fingerprint,
                title = "生物识别解锁",
                subtitle = if (isBiometricEnabled) "使用强生物识别解锁保险库" else "需要先验证现有凭据",
                checked = isBiometricEnabled,
                onCheckedChange = onBiometricEnabledChange
            ),
            switchSettingsGroupItem(
                key = "security.invalidate_biometric",
                visible = isBiometricEnabled,
                icon = Icons.Default.Fingerprint,
                title = "生物识别变更时销毁密钥",
                subtitle = if (isInvalidateKeyOnBioChange)
                    "新增或移除生物识别后，需要重新启用"
                else
                    "系统录入变化后仍保留当前密钥",
                checked = isInvalidateKeyOnBioChange,
                onCheckedChange = onInvalidateKeyOnBioChangeToggle
            )
        )
    )
}
