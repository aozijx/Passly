package com.aozijx.passly.presentation.feature.vault.detail.component

import androidx.compose.runtime.Composable
import com.aozijx.passly.domain.entry.model.Entry
import com.aozijx.passly.domain.entry.model.activity.ActivityType
import com.aozijx.passly.domain.sensitive.OwnedChars
import com.aozijx.passly.presentation.feature.vault.detail.DetailAuthenticate
import com.aozijx.passly.presentation.feature.vault.detail.DetailEditCompletion
import com.aozijx.passly.presentation.feature.vault.detail.DetailEntryPatch
import com.aozijx.passly.presentation.feature.vault.detail.DetailSectionActionHandler
import com.aozijx.passly.presentation.feature.vault.detail.DetailUiAction
import com.aozijx.passly.presentation.feature.vault.detail.DetailUiState
import com.aozijx.passly.presentation.feature.vault.detail.EntryEditState
import com.aozijx.passly.presentation.feature.vault.detail.RevealedFieldKey
import com.aozijx.passly.presentation.feature.vault.detail.copySensitiveField
import com.aozijx.passly.presentation.ui.vault.detail.component.WifiSection
import com.aozijx.passly.presentation.ui.vault.detail.model.DetailWifiUiModel

@Composable
internal fun DetailWifiHost(
    entry: Entry,
    uiState: DetailUiState,
    editState: EntryEditState,
    onAction: (DetailUiAction) -> Unit,
    onAuthenticate: DetailAuthenticate,
    onCopySensitive: (String) -> Unit,
) {
    val handler = DetailSectionActionHandler(onAuthenticate, onAction, onCopySensitive)
    val password = uiState.revealed(RevealedFieldKey.PASSWORD)?.let { String(it.toCharArray()) }
    WifiSection(
        model = DetailWifiUiModel(
            entry.username,
            password,
            password != null,
            editState.isEditingPassword,
            editState.editedPassword,
            entry.secret.wifi?.securityType ?: "WPA",
            entry.secret.wifi?.isHidden ?: false,
        ),
        onSsidCopy = {
            handler.copy(entry.username)
            handler.record("SSID", ActivityType.COPY_PASSWORD)
        },
        onPasswordCopy = {
            copySensitiveField(
                handler,
                "wifi password",
                password?.let(OwnedChars::fromString),
                entry.secret.wifi?.password,
            )
        },
        onPasswordReveal = {
            onAction(DetailUiAction.RevealHighSensitivityField(RevealedFieldKey.PASSWORD))
        },
        onPasswordEditStarted = {
            editState.editedPassword = password.orEmpty()
            editState.isEditingPassword = true
        },
        onPasswordChanged = { editState.editedPassword = it },
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
