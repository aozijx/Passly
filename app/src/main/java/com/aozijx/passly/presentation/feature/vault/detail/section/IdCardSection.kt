package com.aozijx.passly.presentation.feature.vault.detail.section

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
import com.aozijx.passly.presentation.feature.vault.detail.DetailAuthenticate
import com.aozijx.passly.presentation.feature.vault.detail.component.DetailItem
import com.aozijx.passly.presentation.feature.vault.detail.DetailUiAction
import com.aozijx.passly.presentation.feature.vault.detail.DetailSectionActionHandler
import com.aozijx.passly.presentation.feature.vault.detail.copySensitiveField
import com.aozijx.passly.presentation.feature.vault.detail.RevealedFieldKey
import com.aozijx.passly.domain.sensitive.OwnedChars

@Composable
fun IdCardSection(
    entry: Entry,
    hasIdNumber: Boolean,
    revealedIdNumber: String?,
    onIdNumberRevealed: (String?) -> Unit,
    onAuthenticate: DetailAuthenticate,
    onAction: (DetailUiAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val msgCopySuccess = stringResource(R.string.field_copy_success_message)
    val idNumberLabel = stringResource(R.string.id_number)
    val usernameLabel = stringResource(R.string.vault_detail_username)
    val notSet = stringResource(R.string.not_set)
    val actionHandler = DetailSectionActionHandler(
        onAuthenticate = onAuthenticate,
        onAction = onAction
    )

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        DetailItem(
            label = idNumberLabel,
            value = if (hasIdNumber) revealedIdNumber else notSet,
            isRevealed = revealedIdNumber != null || !hasIdNumber,
            onCopy = {
                copySensitiveField(
                    context = context,
                    handler = actionHandler,
                    fieldName = "ID number",
                    revealedValue = revealedIdNumber?.let { OwnedChars.fromString(it) },
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
                else onAction(DetailUiAction.RevealHighSensitivityField(RevealedFieldKey.ID_NUMBER))
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
