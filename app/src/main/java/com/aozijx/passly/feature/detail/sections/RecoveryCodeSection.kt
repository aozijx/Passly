package com.aozijx.passly.feature.detail.sections

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aozijx.passly.R
import com.aozijx.passly.core.platform.ClipboardUtils
import com.aozijx.passly.domain.model.activity.ActivityType
import com.aozijx.passly.domain.model.entry.VaultEntry
import com.aozijx.passly.feature.auth.biometric.BiometricPromptLauncher
import com.aozijx.passly.feature.detail.components.DetailItem
import com.aozijx.passly.feature.detail.contract.DetailEvent
import com.aozijx.passly.feature.detail.internal.DetailSectionActionHandler
import com.aozijx.passly.feature.detail.internal.copySensitiveField
import com.aozijx.passly.feature.detail.internal.toggleRevealSensitiveField

@Composable
fun RecoveryCodeSection(
    launcher: BiometricPromptLauncher,
    entry: VaultEntry,
    revealedRecoveryCodes: String?,
    onRecoveryCodesRevealed: (String?) -> Unit,
    onAuthenticate: (launcher: BiometricPromptLauncher, title: String, subtitle: String, onSuccess: () -> Unit) -> Unit,
    onEvent: (DetailEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val copied = stringResource(R.string.vault_detail_copied)
    val notSet = stringResource(R.string.vault_detail_not_set)
    val hidden = stringResource(R.string.hidden_mask)
    val actionHandler = DetailSectionActionHandler(
        launcher = launcher,
        onAuthenticate = onAuthenticate,
        onEvent = onEvent
    )

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        DetailItem(
            label = stringResource(R.string.passkey_recovery_codes),
            value = when {
                entry.credential.recoveryCodes.isEmpty() -> notSet
                revealedRecoveryCodes != null -> revealedRecoveryCodes
                else -> hidden
            },
            isRevealed = revealedRecoveryCodes != null,
            onCopy = {
                copySensitiveField(
                    context = context,
                    handler = actionHandler,
                    fieldName = "recovery codes",
                    revealedValue = revealedRecoveryCodes,
                    sourceValue = entry.credential.recoveryCodes.joinToString("\n"),
                    authTitle = "解密恢复码",
                    authSubtitle = "验证身份以复制信息",
                    onReveal = onRecoveryCodesRevealed,
                    afterCopy = { Toast.makeText(context, copied, Toast.LENGTH_SHORT).show() }
                )
            },
            onEdit = {
                toggleRevealSensitiveField(
                    handler = actionHandler,
                    fieldName = "recovery codes",
                    revealedValue = revealedRecoveryCodes,
                    sourceValue = entry.credential.recoveryCodes.joinToString("\n"),
                    authTitle = "解密恢复码",
                    authSubtitle = "验证身份以查看信息",
                    onReveal = onRecoveryCodesRevealed
                )
            }
        )

        if (entry.username.isNotBlank()) {
            DetailItem(
                label = stringResource(R.string.vault_detail_username),
                value = entry.username,
                isRevealed = true,
                onCopy = {
                    ClipboardUtils.copy(context, entry.username)
                    Toast.makeText(context, copied, Toast.LENGTH_SHORT).show()
                    actionHandler.record(
                        "username",
                        ActivityType.COPY_PASSWORD
                    )
                },
                onEdit = {}
            )
        }
    }
}