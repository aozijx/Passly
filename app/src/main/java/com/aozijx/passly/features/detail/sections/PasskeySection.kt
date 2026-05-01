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
import com.aozijx.passly.features.detail.contract.RevealedFieldKey

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
    val hidden = stringResource(R.string.label_hidden_mask)

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
                val encrypted = entry.passkeyDataJson
                if (encrypted.isNullOrBlank()) return@DetailItem
                if (revealedPasskeyData != null) {
                    ClipboardUtils.copy(context, revealedPasskeyData)
                    Toast.makeText(context, copied, Toast.LENGTH_SHORT).show()
                    onEvent(DetailEvent.RecordAction("passkey data", VaultHistory.HistoryType.COPY))
                } else {
                    onAuthenticate(activity, "解密 Passkey 数据", "验证身份以复制数据") {
                        try {
                            onRevealField(RevealedFieldKey.PASSKEY_DATA, encrypted)
                            ClipboardUtils.copy(context, encrypted)
                            Toast.makeText(context, copied, Toast.LENGTH_SHORT).show()
                            onEvent(
                                DetailEvent.RecordAction(
                                    "passkey data",
                                    VaultHistory.HistoryType.COPY
                                )
                            )
                        } catch (_: Exception) {
                        }
                    }
                }
            },
            onEdit = {
                val encrypted = entry.passkeyDataJson
                if (encrypted.isNullOrBlank()) return@DetailItem
                if (revealedPasskeyData != null) {
                    onRevealField(RevealedFieldKey.PASSKEY_DATA, null)
                } else {
                    onAuthenticate(activity, "解密 Passkey 数据", "验证身份以查看数据") {
                        try {
                            onRevealField(RevealedFieldKey.PASSKEY_DATA, encrypted)
                            onEvent(
                                DetailEvent.RecordAction(
                                    "passkey data",
                                    VaultHistory.HistoryType.ACCESS
                                )
                            )
                        } catch (_: Exception) {
                        }
                    }
                }
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
                    onAuthenticate(activity, "解密恢复码", "验证身份以复制恢复码") {
                        try {
                            onRevealField(RevealedFieldKey.RECOVERY_CODES, encrypted)
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
                    onRevealField(RevealedFieldKey.RECOVERY_CODES, null)
                } else {
                    onAuthenticate(activity, "解密恢复码", "验证身份以查看恢复码") {
                        try {
                            onRevealField(RevealedFieldKey.RECOVERY_CODES, encrypted)
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

        if (!entry.hardwareKeyInfo.isNullOrBlank()) {
            DetailItem(
                label = stringResource(R.string.hardware_key_info),
                value = entry.hardwareKeyInfo,
                isRevealed = true,
                onCopy = {
                    ClipboardUtils.copy(context, entry.hardwareKeyInfo)
                    Toast.makeText(context, copied, Toast.LENGTH_SHORT).show()
                    onEvent(
                        DetailEvent.RecordAction(
                            "hardware key info",
                            VaultHistory.HistoryType.COPY
                        )
                    )
                },
                onEdit = {}
            )
        }
    }
}