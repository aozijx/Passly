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
import com.aozijx.passly.domain.entry.model.Entry
import com.aozijx.passly.domain.entry.model.activity.ActivityType
import com.aozijx.passly.feature.detail.DetailAuthenticate
import com.aozijx.passly.feature.detail.ui.components.DetailItem
import com.aozijx.passly.feature.detail.contract.DetailIntent
import com.aozijx.passly.feature.detail.internal.DetailSectionActionHandler
import com.aozijx.passly.feature.detail.internal.copySensitiveField
import com.aozijx.passly.feature.detail.contract.RevealedFieldKey

@Composable
fun IdCardSection(
    entry: Entry,
    hasIdNumber: Boolean,
    revealedIdNumber: String?,
    onIdNumberRevealed: (String?) -> Unit,
    onAuthenticate: DetailAuthenticate,
    onEvent: (DetailIntent) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val msgCopySuccess = stringResource(R.string.msg_copy_success)
    val idNumberLabel = stringResource(R.string.id_number)
    val usernameLabel = stringResource(R.string.vault_detail_username)
    val notSet = stringResource(R.string.not_set)
    val actionHandler = DetailSectionActionHandler(
        onAuthenticate = onAuthenticate,
        onEvent = onEvent
    )

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        DetailItem(
            label = idNumberLabel,
            value = when {
                !hasIdNumber -> notSet
                revealedIdNumber != null -> revealedIdNumber
                else -> HiddenMask.DEFAULT
            },
            isRevealed = revealedIdNumber != null,
            onCopy = {
                copySensitiveField(
                    context = context,
                    handler = actionHandler,
                    fieldName = "ID number",
                    revealedValue = revealedIdNumber,
                    sourceValue = null,
                    afterCopy = {
                        Toast.makeText(
                            context,
                            msgCopySuccess.format(idNumberLabel),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                )
            },
            onEdit = null,
            onReveal = {
                if (revealedIdNumber != null) onIdNumberRevealed(null)
                else onEvent(DetailIntent.RevealHighSensitivityField(RevealedFieldKey.ID_NUMBER))
            }
        )

        if (entry.username.isNotBlank()) {
            DetailItem(
                label = usernameLabel,
                value = entry.username,
                isRevealed = true,
                onCopy = {
                    ClipboardUtils.copy(context, entry.username)
                    Toast.makeText(
                        context,
                        msgCopySuccess.format(usernameLabel),
                        Toast.LENGTH_SHORT
                    ).show()
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
