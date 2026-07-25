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
import com.aozijx.passly.domain.entry.model.VaultEntry
import com.aozijx.passly.domain.entry.model.activity.ActivityType
import com.aozijx.passly.feature.detail.components.InfoGroupCard
import com.aozijx.passly.feature.detail.components.TotpCodeCard
import com.aozijx.passly.feature.detail.contract.DetailIntent
import com.aozijx.passly.feature.detail.internal.TotpEditState
import com.aozijx.passly.feature.detail.sections.dialogs.EditTotpSection
import com.aozijx.passly.feature.vault.model.OtpUiState

@Composable
fun TotpSection(
    modifier: Modifier = Modifier,
    entry: VaultEntry,
    currentState: OtpUiState?,
    isSteam: Boolean,
    isHotp: Boolean = false,
    totpEditState: TotpEditState,
    showQrDialog: () -> Unit,
    onEntryUpdated: (VaultEntry) -> Unit,
    onEvent: (DetailIntent) -> Unit,
    onGenerateHotpCode: ((entryId: String) -> Unit)? = null
) {
    val context = LocalContext.current
    val msgCopySuccess = stringResource(R.string.msg_copy_success)
    val totpLabel = stringResource(R.string.vault_detail_totp_label)

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        TotpCodeCard(
            currentState = currentState,
            isSteam = isSteam,
            showProgress = !isHotp,
            onQrClick = showQrDialog,
            onCodeClick = {
                if (isHotp) {
                    // HOTP：先生成再复制
                    onGenerateHotpCode?.invoke(entry.id)
                }
                currentState?.let { state ->
                    val code = state.code
                    if (!code.isNullOrEmpty() && !code.contains("-")) {
                        ClipboardUtils.copy(context, code)
                        Toast.makeText(
                            context,
                            msgCopySuccess.format(totpLabel),
                            Toast.LENGTH_SHORT
                        ).show()
                        onEvent(DetailIntent.RecordAction("totp", ActivityType.COPY_PASSWORD))
                    }
                }
            },
            title = totpLabel
        )

        if (totpEditState.isEditing && entry.secret.otp?.config?.secret != null) {
            InfoGroupCard(title = stringResource(R.string.vault_edit_totp_title)) {
                EditTotpSection(
                    item = entry,
                    editState = totpEditState,
                    onEntryUpdated = onEntryUpdated
                )
            }
        }
    }
}