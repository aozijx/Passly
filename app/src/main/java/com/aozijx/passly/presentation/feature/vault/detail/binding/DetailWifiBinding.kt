package com.aozijx.passly.presentation.feature.vault.detail.binding

import androidx.compose.runtime.Composable
import com.aozijx.passly.domain.entry.model.Entry
import com.aozijx.passly.domain.entry.model.FieldKey
import com.aozijx.passly.presentation.feature.vault.detail.DetailEditCompletion
import com.aozijx.passly.feature.vault.detail.DetailEntryPatch
import com.aozijx.passly.presentation.feature.vault.detail.DetailSectionActionHandler
import com.aozijx.passly.presentation.feature.vault.detail.DetailUiAction
import com.aozijx.passly.presentation.feature.vault.detail.DetailUiState
import com.aozijx.passly.presentation.feature.vault.detail.RevealedFieldKey
import com.aozijx.passly.presentation.ui.vault.detail.component.WifiSection
import com.aozijx.passly.presentation.ui.vault.detail.model.DetailWifiUiModel

@Composable
internal fun DetailWifiBinding(
    entry: Entry,
    uiState: DetailUiState,
    onAction: (DetailUiAction) -> Unit,
) {
    val handler = DetailSectionActionHandler(onAction)
    val password = uiState.revealed(RevealedFieldKey.PASSWORD)?.let { String(it.toCharArray()) }
    WifiSection(
        model = DetailWifiUiModel(
            entry.username,
            password,
            password != null,
            uiState.fieldEdits.isEditing(RevealedFieldKey.PASSWORD),
            uiState.fieldEdits.draft(RevealedFieldKey.PASSWORD),
            entry.secret.wifi?.securityType ?: "WPA",
            entry.secret.wifi?.isHidden ?: false,
        ),
        onSsidCopy = { handler.copy(FieldKey.WIFI_SSID) },
        onPasswordCopy = { handler.copy(FieldKey.PASSWORD) },
        onPasswordReveal = {
            onAction(DetailUiAction.ToggleFieldVisibility(RevealedFieldKey.PASSWORD))
        },
        onPasswordEditStarted = {
            onAction(DetailUiAction.StartFieldEdit(RevealedFieldKey.PASSWORD, password.orEmpty()))
        },
        onPasswordChanged = { onAction(DetailUiAction.UpdateFieldDraft(RevealedFieldKey.PASSWORD, it)) },
        onPasswordSaved = {
            if (it != password) {
                onAction(
                    DetailUiAction.CommitPatch(
                        DetailEntryPatch.WifiPassword(it),
                        DetailEditCompletion.SensitiveField(RevealedFieldKey.PASSWORD),
                    ),
                )
            }
        },
    )
}
