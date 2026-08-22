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
import com.aozijx.passly.domain.entry.model.Entry
import com.aozijx.passly.domain.entry.model.activity.ActivityType
import com.aozijx.passly.feature.detail.DetailAuthenticate
import com.aozijx.passly.feature.detail.ui.components.DetailItem
import com.aozijx.passly.feature.detail.contract.DetailUiAction
import com.aozijx.passly.feature.detail.contract.RevealedFieldKey
import com.aozijx.passly.feature.detail.internal.DetailSectionActionHandler
import com.aozijx.passly.feature.detail.internal.copySensitiveField
import com.aozijx.passly.domain.sensitive.OwnedChars

@Composable
fun PasskeySection(
    entry: Entry,
    hasPasskeyData: Boolean,
    revealedPasskeyData: String?,
    onRevealField: (String, String?) -> Unit,
    onAuthenticate: DetailAuthenticate,
    onAction: (DetailUiAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val msgCopySuccess = stringResource(R.string.field_copy_success_message)
    val passkeyDataLabel = stringResource(R.string.passkey_data)
    val hardwareKeyInfoLabel = stringResource(R.string.hardware_key_info)
    val notSet = stringResource(R.string.not_set)
    val actionHandler = DetailSectionActionHandler(
        onAuthenticate = onAuthenticate,
        onAction = onAction
    )

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        DetailItem(
            label = passkeyDataLabel,
            value = if (hasPasskeyData) revealedPasskeyData else notSet,
            isRevealed = revealedPasskeyData != null || !hasPasskeyData,
            onCopy = {
                copySensitiveField(
                    context = context,
                    handler = actionHandler,
                    fieldName = "passkey data",
                    revealedValue = revealedPasskeyData?.let { OwnedChars.fromString(it) },
                    sourceValue = null,
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
                if (revealedPasskeyData != null) {
                    onRevealField(RevealedFieldKey.PASSKEY_DATA, null)
                } else {
                    onAction(
                        DetailUiAction.RevealHighSensitivityField(RevealedFieldKey.PASSKEY_DATA)
                    )
                }
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
