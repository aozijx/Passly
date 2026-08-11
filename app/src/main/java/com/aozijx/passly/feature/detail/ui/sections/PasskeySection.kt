package com.aozijx.passly.feature.detail.ui.sections

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
import com.aozijx.passly.core.ui.components.HiddenMask
import com.aozijx.passly.domain.authentication.SensitiveAccessLevel
import com.aozijx.passly.domain.entry.model.VaultEntry
import com.aozijx.passly.domain.entry.model.activity.ActivityType
import com.aozijx.passly.feature.detail.DetailAuthenticate
import com.aozijx.passly.feature.detail.ui.components.DetailItem
import com.aozijx.passly.feature.detail.contract.DetailIntent
import com.aozijx.passly.feature.detail.contract.RevealedFieldKey
import com.aozijx.passly.feature.detail.internal.DetailSectionActionHandler
import com.aozijx.passly.feature.detail.internal.copySensitiveField
import com.aozijx.passly.feature.detail.internal.toggleRevealSensitiveField

@Composable
fun PasskeySection(
    entry: VaultEntry,
    revealedPasskeyData: String?,
    onRevealField: (String, String?) -> Unit,
    onAuthenticate: DetailAuthenticate,
    onEvent: (DetailIntent) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val msgCopySuccess = stringResource(R.string.msg_copy_success)
    val passkeyDataLabel = stringResource(R.string.passkey_data)
    val hardwareKeyInfoLabel = stringResource(R.string.hardware_key_info)
    val notSet = stringResource(R.string.not_set)
    val actionHandler = DetailSectionActionHandler(
        onAuthenticate = onAuthenticate,
        onEvent = onEvent
    )

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        DetailItem(
            label = passkeyDataLabel,
            value = when {
                entry.secret.passkey?.privateKeyReference.isNullOrBlank() -> notSet
                revealedPasskeyData != null -> revealedPasskeyData
                else -> HiddenMask.DEFAULT
            },
            isRevealed = revealedPasskeyData != null,
            onCopy = {
                copySensitiveField(
                    context = context,
                    handler = actionHandler,
                    fieldName = "passkey data",
                    revealedValue = revealedPasskeyData,
                    sourceValue = entry.secret.passkey?.privateKeyReference,
                    afterCopy = {
                        Toast.makeText(
                            context,
                            msgCopySuccess.format(passkeyDataLabel),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                )
            },
            onEdit = null,
            onReveal = {
                toggleRevealSensitiveField(
                    handler = actionHandler,
                    fieldName = "passkey data",
                    revealedValue = revealedPasskeyData,
                    sourceValue = entry.secret.passkey?.privateKeyReference,
                    accessLevel = SensitiveAccessLevel.HIGH,
                    onReveal = { onRevealField(RevealedFieldKey.PASSKEY_DATA, it) }
                )
            }
        )

        val hardwareKeyInfo = entry.secret.passkey?.hardwareKeyInfo
        if (!hardwareKeyInfo.isNullOrBlank()) {
            DetailItem(
                label = hardwareKeyInfoLabel,
                value = hardwareKeyInfo,
                isRevealed = true,
                onCopy = {
                    ClipboardUtils.copy(
                        context,
                        hardwareKeyInfo
                    )
                    Toast.makeText(
                        context,
                        msgCopySuccess.format(hardwareKeyInfoLabel),
                        Toast.LENGTH_SHORT
                    ).show()
                    actionHandler.record(
                        "hardware key info",
                        ActivityType.COPY_PASSWORD
                    )
                },
                onEdit = {}
            )
        }
    }
}
