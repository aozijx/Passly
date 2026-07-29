package com.aozijx.passly.feature.detail.sections

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
import com.aozijx.passly.feature.detail.components.DetailItem
import com.aozijx.passly.feature.detail.contract.DetailIntent
import com.aozijx.passly.feature.detail.internal.DetailSectionActionHandler
import com.aozijx.passly.feature.detail.internal.copySensitiveField
import com.aozijx.passly.feature.detail.internal.toggleRevealSensitiveField

@Composable
fun IdCardSection(
    entry: VaultEntry,
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
                entry.secret.identity?.idNumber.isNullOrBlank() -> notSet
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
                    sourceValue = entry.secret.identity?.idNumber,
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
                toggleRevealSensitiveField(
                    handler = actionHandler,
                    fieldName = "ID number",
                    revealedValue = revealedIdNumber,
                    sourceValue = entry.secret.identity?.idNumber,
                    accessLevel = SensitiveAccessLevel.HIGH,
                    onReveal = onIdNumberRevealed
                )
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
