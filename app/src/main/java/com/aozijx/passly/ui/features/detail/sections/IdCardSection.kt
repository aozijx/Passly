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
import com.aozijx.passly.ui.features.detail.internal.DetailSectionActionHandler
import com.aozijx.passly.ui.features.detail.internal.copySensitiveField
import com.aozijx.passly.ui.features.detail.internal.toggleRevealSensitiveField

@Composable
fun IdCardSection(
    activity: FragmentActivity,
    entry: VaultEntry,
    revealedIdNumber: String?,
    onIdNumberRevealed: (String?) -> Unit,
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
            label = stringResource(R.string.id_number),
            value = when {
                entry.idNumber.isNullOrBlank() -> notSet
                revealedIdNumber != null -> revealedIdNumber
                else -> hidden
            },
            isRevealed = revealedIdNumber != null,
            onCopy = {
                copySensitiveField(
                    context = context,
                    handler = actionHandler,
                    fieldName = "ID number",
                    revealedValue = revealedIdNumber,
                    sourceValue = entry.idNumber,
                    authTitle = "解密身份证号",
                    authSubtitle = "验证身份以复制信息",
                    onReveal = onIdNumberRevealed,
                    afterCopy = { Toast.makeText(context, copied, Toast.LENGTH_SHORT).show() }
                )
            },
            onEdit = {
                toggleRevealSensitiveField(
                    handler = actionHandler,
                    fieldName = "ID number",
                    revealedValue = revealedIdNumber,
                    sourceValue = entry.idNumber,
                    authTitle = "解密身份证号",
                    authSubtitle = "验证身份以查看信息",
                    onReveal = onIdNumberRevealed
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
                        VaultHistory.HistoryType.COPY
                    )
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
                    ClipboardUtils.copy(
                        context,
                        entry.cardExpiration
                    )
                    Toast.makeText(context, copied, Toast.LENGTH_SHORT).show()
                    actionHandler.record(
                        "expiration",
                        VaultHistory.HistoryType.COPY
                    )
                },
                onEdit = {}
            )
        }
    }
}