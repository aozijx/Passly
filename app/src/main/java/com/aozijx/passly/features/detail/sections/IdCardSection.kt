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
fun IdCardSection(
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

    var revealedIdNumber by remember { mutableStateOf<String?>(null) }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        DetailItem(
            label = stringResource(R.string.id_number),
            value = when {
                entry.idNumber.isNullOrBlank() -> notSet
                revealedIdNumber != null -> revealedIdNumber!!
                else -> hidden
            },
            isRevealed = revealedIdNumber != null,
            onCopy = {
                val encrypted = entry.idNumber
                if (encrypted.isNullOrBlank()) return@DetailItem
                val cached = revealedIdNumber
                if (cached != null) {
                    ClipboardUtils.copy(context, cached)
                    Toast.makeText(context, copied, Toast.LENGTH_SHORT).show()
                } else {
                    onAuthenticate(activity, "解密身份证号", "验证身份以复制信息", {
                        try {
                            val decrypted = CryptoManager.decrypt(encrypted)
                            revealedIdNumber = decrypted
                            ClipboardUtils.copy(context, decrypted)
                            Toast.makeText(context, copied, Toast.LENGTH_SHORT).show()
                        } catch (e: Exception) {}
                    })
                }
            },
            onEdit = {
                val encrypted = entry.idNumber
                if (encrypted.isNullOrBlank()) return@DetailItem
                if (revealedIdNumber != null) {
                    revealedIdNumber = null
                } else {
                    onAuthenticate(activity, "解密身份证号", "验证身份以查看信息", {
                        try {
                            revealedIdNumber = CryptoManager.decrypt(encrypted)
                        } catch (e: Exception) {}
                    })
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
                },
                onEdit = {}
            )
        }

        if (!entry.cardExpiration.isNullOrBlank()) {
            DetailItem(
                label = stringResource(R.string.card_expiration),
                value = entry.cardExpiration,
                isRevealed = true,
                onCopy = {
                    ClipboardUtils.copy(context, entry.cardExpiration)
                    Toast.makeText(context, copied, Toast.LENGTH_SHORT).show()
                },
                onEdit = {}
            )
        }
    }
}