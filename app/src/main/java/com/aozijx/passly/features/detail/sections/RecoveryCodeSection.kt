package com.aozijx.passly.features.detail.sections

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
import com.aozijx.passly.domain.model.core.VaultEntry
import com.aozijx.passly.domain.model.core.VaultHistory
import com.aozijx.passly.features.detail.components.DetailItem
import com.aozijx.passly.features.detail.contract.DetailEvent

@Composable
fun RecoveryCodeSection(
    activity: FragmentActivity,
    entry: VaultEntry,
    revealedRecoveryCodes: String?,
    onRecoveryCodesRevealed: (String?) -> Unit,
    onAuthenticate: (activity: FragmentActivity, title: String, subtitle: String, onSuccess: () -> Unit) -> Unit,
    onEvent: (DetailEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val copied = stringResource(R.string.vault_detail_copied)
    val notSet = stringResource(R.string.vault_detail_not_set)
    val hidden = stringResource(R.string.label_hidden_mask)

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        DetailItem(
            label = stringResource(R.string.passkey_recovery_codes),
            value = when {
                entry.recoveryCodes.isNullOrBlank() -> notSet
                revealedRecoveryCodes != null -> revealedRecoveryCodes
                else -> hidden
            },
            isRevealed = revealedRecoveryCodes != null,
            onCopy = {
                val encrypted = entry.recoveryCodes
                if (encrypted.isNullOrBlank()) return@DetailItem
                if (revealedRecoveryCodes != null) {
                    ClipboardUtils.copy(context, revealedRecoveryCodes)
                    Toast.makeText(context, copied, Toast.LENGTH_SHORT).show()
                    onEvent(
                        DetailEvent.RecordAction(
                            "recovery codes",
                            VaultHistory.HistoryType.COPY
                        )
                    )
                } else {
                    onAuthenticate(activity, "解密恢复码", "验证身份以复制信息") {
                        try {
                            onRecoveryCodesRevealed(encrypted)
                            ClipboardUtils.copy(context, encrypted)
                            Toast.makeText(context, copied, Toast.LENGTH_SHORT).show()
                            onEvent(
                                DetailEvent.RecordAction(
                                    "recovery codes",
                                    VaultHistory.HistoryType.COPY
                                )
                            )
                        } catch (_: Exception) {
                        }
                    }
                }
            },
            onEdit = {
                val encrypted = entry.recoveryCodes
                if (encrypted.isNullOrBlank()) return@DetailItem
                if (revealedRecoveryCodes != null) {
                    onRecoveryCodesRevealed(null)
                } else {
                    onAuthenticate(activity, "解密恢复码", "验证身份以查看信息") {
                        try {
                            onRecoveryCodesRevealed(encrypted)
                            onEvent(
                                DetailEvent.RecordAction(
                                    "recovery codes",
                                    VaultHistory.HistoryType.ACCESS
                                )
                            )
                        } catch (_: Exception) {
                        }
                    }
                }
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
                    onEvent(DetailEvent.RecordAction("username", VaultHistory.HistoryType.COPY))
                },
                onEdit = {}
            )
        }
    }
}