package com.aozijx.passly.features.detail.sections

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aozijx.passly.R
import com.aozijx.passly.core.designsystem.model.TotpState
import com.aozijx.passly.core.platform.ClipboardUtils
import com.aozijx.passly.domain.model.core.VaultEntry
import com.aozijx.passly.features.detail.components.InfoGroupCard
import com.aozijx.passly.features.detail.components.TotpCodeCard
import com.aozijx.passly.features.detail.internal.TotpEditState
import com.aozijx.passly.features.detail.sections.dialogs.EditTotpSection
import com.aozijx.passly.features.vault.VaultViewModel

@Composable
fun TotpSection(
    modifier: Modifier = Modifier,
    entry: VaultEntry,
    currentState: TotpState?,
    isSteam: Boolean,
    totpEditState: TotpEditState,
    showQrDialog: () -> Unit,
    vaultViewModel: VaultViewModel,
    onEntryUpdated: (VaultEntry) -> Unit = vaultViewModel::updateVaultEntry
) {
    val context = LocalContext.current
    val totpCopiedMsg = stringResource(R.string.vault_totp_copied)
    val totpLabel = stringResource(R.string.vault_detail_totp_label)

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        TotpCodeCard(
            currentState = currentState,
            isSteam = isSteam,
            onQrClick = showQrDialog,
            onCodeClick = {
                currentState?.let {
                    if (it.code.isNotEmpty() && !it.code.contains("-")) {
                        ClipboardUtils.copy(context, it.code)
                        Toast.makeText(context, totpCopiedMsg, Toast.LENGTH_SHORT).show()
                    }
                }
            },
            title = totpLabel
        )

        if (totpEditState.isEditing && currentState?.decryptedSecret != null) {
            InfoGroupCard(title = stringResource(R.string.vault_edit_totp_title)) {
                EditTotpSection(entry, vaultViewModel, totpEditState, onEntryUpdated)
            }
        }
    }
}