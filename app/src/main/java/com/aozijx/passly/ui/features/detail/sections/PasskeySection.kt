package com.aozijx.passly.ui.features.detail.sections

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.aozijx.passly.R
import com.aozijx.passly.core.platform.ClipboardUtils
import com.aozijx.passly.domain.model.VaultEntry
import com.aozijx.passly.domain.model.VaultHistory
import com.aozijx.passly.ui.features.detail.components.DetailItem
import com.aozijx.passly.ui.features.detail.contract.DetailEvent
import com.aozijx.passly.ui.features.detail.contract.RevealedFieldKey
import com.aozijx.passly.ui.features.detail.internal.DetailSectionActionHandler
import com.aozijx.passly.ui.features.detail.internal.copySensitiveField
import com.aozijx.passly.ui.features.detail.internal.toggleRevealSensitiveField

@Composable
fun PasskeySection(
    activity: FragmentActivity,
    entry: VaultEntry,
    revealedPasskeyData: String?,
    revealedRecoveryCodes: String?,
    onRevealField: (String, String?) -> Unit,
    onAuthenticate: (activity: FragmentActivity, title: String, subtitle: String, onSuccess: () -> Unit) -> Unit,
    onEvent: (DetailEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val copied = stringResource(R.string.vault_detail_copied)
    val notSet = stringResource(R.string.vault_detail_not_set)
    val hidden = stringResource(R.string.hidden_mask)
    val actionHandler = DetailSectionActionHandler(
        activity = activity,
        onAuthenticate = onAuthenticate,
        onEvent = onEvent
    )

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        DetailItem(
            label = stringResource(R.string.passkey_data),
            value = when {
                entry.passkeyDataJson.isNullOrBlank() -> notSet
                revealedPasskeyData != null -> revealedPasskeyData
                else -> hidden
            },
            isRevealed = revealedPasskeyData != null,
            onCopy = {
                copySensitiveField(
                    context = context,
                    handler = actionHandler,
                    fieldName = "passkey data",
                    revealedValue = revealedPasskeyData,
                    sourceValue = entry.passkeyDataJson,
                    authTitle = "解密 Passkey 数据",
                    authSubtitle = "验证身份以复制数据",
                    onReveal = { onRevealField(RevealedFieldKey.PASSKEY_DATA, it) },
                    afterCopy = { Toast.makeText(context, copied, Toast.LENGTH_SHORT).show() }
                )
            },
            onEdit = {
                toggleRevealSensitiveField(
                    handler = actionHandler,
                    fieldName = "passkey data",
                    revealedValue = revealedPasskeyData,
                    sourceValue = entry.passkeyDataJson,
                    authTitle = "解密 Passkey 数据",
                    authSubtitle = "验证身份以查看数据",
                    onReveal = { onRevealField(RevealedFieldKey.PASSKEY_DATA, it) }
                )
            }
        )

        DetailItem(
            label = stringResource(R.string.passkey_recovery_codes),
            value = when {
                entry.recoveryCodes.isNullOrBlank() -> notSet
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
                    sourceValue = entry.recoveryCodes,
                    authTitle = "解密恢复码",
                    authSubtitle = "验证身份以复制恢复码",
                    onReveal = { onRevealField(RevealedFieldKey.RECOVERY_CODES, it) },
                    afterCopy = { Toast.makeText(context, copied, Toast.LENGTH_SHORT).show() }
                )
            },
            onEdit = {
                toggleRevealSensitiveField(
                    handler = actionHandler,
                    fieldName = "recovery codes",
                    revealedValue = revealedRecoveryCodes,
                    sourceValue = entry.recoveryCodes,
                    authTitle = "解密恢复码",
                    authSubtitle = "验证身份以查看恢复码",
                    onReveal = { onRevealField(RevealedFieldKey.RECOVERY_CODES, it) }
                )
            }
        )

        if (!entry.hardwareKeyInfo.isNullOrBlank()) {
            DetailItem(
                label = stringResource(R.string.hardware_key_info),
                value = entry.hardwareKeyInfo,
                isRevealed = true,
                onCopy = {
                    ClipboardUtils.copy(
                        context,
                        entry.hardwareKeyInfo
                    )
                    Toast.makeText(context, copied, Toast.LENGTH_SHORT).show()
                    actionHandler.record(
                        "hardware key info",
                        VaultHistory.HistoryType.COPY
                    )
                },
                onEdit = {}
            )
        }
    }
}