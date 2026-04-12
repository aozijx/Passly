package com.aozijx.passly.features.detail.sections

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.aozijx.passly.R
import com.aozijx.passly.core.crypto.CryptoManager
import com.aozijx.passly.core.platform.ClipboardUtils
import com.aozijx.passly.domain.model.core.VaultEntry
import com.aozijx.passly.features.detail.components.DetailItem

@Composable
fun PasskeySection(
    activity: FragmentActivity,
    entry: VaultEntry,
    onUpdateVaultEntry: (VaultEntry) -> Unit,
    onAuthenticate: (activity: FragmentActivity, title: String, subtitle: String, onSuccess: () -> Unit) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val copied = stringResource(R.string.vault_detail_copied)
    val notSet = stringResource(R.string.vault_detail_not_set)
    val hidden = stringResource(R.string.label_hidden_mask)

    var revealedPasskeyData by remember { mutableStateOf<String?>(null) }
    var revealedRecoveryCodes by remember { mutableStateOf<String?>(null) }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        DetailItem(
            label = stringResource(R.string.passkey_data),
            value = when {
                entry.passkeyDataJson.isNullOrBlank() -> notSet
                revealedPasskeyData != null -> revealedPasskeyData!!
                else -> hidden
            },
            isRevealed = revealedPasskeyData != null,
            onCopy = {
                val encrypted = entry.passkeyDataJson
                if (encrypted.isNullOrBlank()) return@DetailItem
                val cached = revealedPasskeyData
                if (cached != null) {
                    ClipboardUtils.copy(context, cached)
                    Toast.makeText(context, copied, Toast.LENGTH_SHORT).show()
                } else {
                    onAuthenticate(activity, "解密 Passkey 数据", "验证身份以复制数据", {
                        try {
                            val decrypted = CryptoManager.decrypt(encrypted)
                            revealedPasskeyData = decrypted
                            ClipboardUtils.copy(context, decrypted)
                            Toast.makeText(context, copied, Toast.LENGTH_SHORT).show()
                        } catch (e: Exception) {}
                    })
                }
            },
            onEdit = {
                val encrypted = entry.passkeyDataJson
                if (encrypted.isNullOrBlank()) return@DetailItem
                if (revealedPasskeyData != null) {
                    revealedPasskeyData = null
                } else {
                    onAuthenticate(activity, "解密 Passkey 数据", "验证身份以查看数据", {
                        try {
                            revealedPasskeyData = CryptoManager.decrypt(encrypted)
                        } catch (e: Exception) {}
                    })
                }
            }
        )

        DetailItem(
            label = stringResource(R.string.passkey_recovery_codes),
            value = when {
                entry.recoveryCodes.isNullOrBlank() -> notSet
                revealedRecoveryCodes != null -> revealedRecoveryCodes!!
                else -> hidden
            },
            isRevealed = revealedRecoveryCodes != null,
            onCopy = {
                val encrypted = entry.recoveryCodes
                if (encrypted.isNullOrBlank()) return@DetailItem
                val cached = revealedRecoveryCodes
                if (cached != null) {
                    ClipboardUtils.copy(context, cached)
                    Toast.makeText(context, copied, Toast.LENGTH_SHORT).show()
                } else {
                    onAuthenticate(activity, "解密恢复码", "验证身份以复制恢复码", {
                        try {
                            val decrypted = CryptoManager.decrypt(encrypted)
                            revealedRecoveryCodes = decrypted
                            ClipboardUtils.copy(context, decrypted)
                            Toast.makeText(context, copied, Toast.LENGTH_SHORT).show()
                        } catch (e: Exception) {}
                    })
                }
            },
            onEdit = {
                val encrypted = entry.recoveryCodes
                if (encrypted.isNullOrBlank()) return@DetailItem
                if (revealedRecoveryCodes != null) {
                    revealedRecoveryCodes = null
                } else {
                    onAuthenticate(activity, "解密恢复码", "验证身份以查看恢复码", {
                        try {
                            revealedRecoveryCodes = CryptoManager.decrypt(encrypted)
                        } catch (e: Exception) {}
                    })
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
                },
                onEdit = {}
            )
        }
    }
}