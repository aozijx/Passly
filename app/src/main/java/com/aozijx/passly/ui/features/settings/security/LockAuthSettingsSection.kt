package com.aozijx.passly.ui.features.settings.security

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import com.aozijx.passly.domain.model.AppDefaults
import com.aozijx.passly.ui.features.settings.components.navigationSettingsItem
import com.aozijx.passly.ui.features.settings.components.switchSettingsItem
import com.aozijx.passly.ui.features.settings.shell.SettingsGroupTitle
import com.aozijx.passly.ui.features.settings.shell.SettingsRoundedGroup
import kotlin.math.roundToInt

private const val SLIDER_MIN_SECONDS = (AppDefaults.Lock.SLIDER_MIN_TIMEOUT_MS / 1000L).toFloat()
private const val SLIDER_MAX_SECONDS = (AppDefaults.Lock.MAX_TIMEOUT_MS / 1000L).toFloat()
private const val SLIDER_STEP_SECONDS = (AppDefaults.Lock.SLIDER_STEP_MS / 1000L).toFloat()

@Composable
fun LockAuthSettingsSection(
    lockTimeout: Long,
    isAppPasswordEnabled: Boolean,
    isInvalidateKeyOnBioChange: Boolean,
    isLockOnBackground: Boolean,
    onLockTimeoutChange: (Long) -> Unit,
    onAppPasswordClick: () -> Unit,
    onInvalidateKeyOnBioChangeToggle: (Boolean) -> Unit,
    onLockOnBackgroundChange: (Boolean) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val currentSeconds = (lockTimeout / 1000L).toFloat()
        .coerceIn(SLIDER_MIN_SECONDS, SLIDER_MAX_SECONDS)
    var sliderValue by remember(lockTimeout) { mutableFloatStateOf(currentSeconds) }

    SettingsGroupTitle(text = "认证")
    SettingsRoundedGroup {
        navigationSettingsItem(
            icon = Icons.Default.Timer,
            title = "自动锁定",
            value = formatLockTimeoutText(lockTimeout),
            onClick = { expanded = !expanded }
        )
        item(visible = expanded) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
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
                Spacer(modifier = Modifier.height(4.dp))
            }
        }
        navigationSettingsItem(
            icon = Icons.Default.Lock,
            title = "设置密码",
            subtitle = "独立于系统锁屏，用密码直接解锁应用",
            value = if (isAppPasswordEnabled) "已设置" else "未设置",
            onClick = onAppPasswordClick
        )
        switchSettingsItem(
            icon = Icons.Default.Fingerprint,
            title = "生物识别变更时销毁密钥",
            subtitle = if (isInvalidateKeyOnBioChange)
                "新增或移除指纹/面部时，保险箱密钥将被销毁"
            else
                "新增或移除指纹/面部时，保险箱密钥保持有效",
            checked = isInvalidateKeyOnBioChange,
            onCheckedChange = onInvalidateKeyOnBioChangeToggle
        )
        switchSettingsItem(
            icon = Icons.Default.Lock,
            title = "立即锁定",
            subtitle = "退出 app 后将在设定的锁定时间后锁定",
            checked = isLockOnBackground,
            onCheckedChange = onLockOnBackgroundChange
        )
    }
}