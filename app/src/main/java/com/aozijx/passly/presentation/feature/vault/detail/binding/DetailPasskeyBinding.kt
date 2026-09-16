package com.aozijx.passly.presentation.feature.vault.detail.binding

import androidx.compose.runtime.Composable
import com.aozijx.passly.domain.entry.model.Entry
import com.aozijx.passly.domain.entry.model.FieldKey
import com.aozijx.passly.domain.entry.model.sensitive.SensitiveFieldKey
import com.aozijx.passly.presentation.feature.vault.detail.DetailUiAction
import com.aozijx.passly.presentation.feature.vault.detail.DetailUiState
import com.aozijx.passly.presentation.feature.vault.detail.RevealedFieldKey
import com.aozijx.passly.presentation.ui.vault.detail.component.PasskeySection

@Composable
internal fun DetailPasskeyBinding(
    entry: Entry,
    uiState: DetailUiState,
    onAction: (DetailUiAction) -> Unit,
) {
    val passkeyData = uiState.revealed(RevealedFieldKey.PASSKEY_DATA)
        ?.let { String(it.toCharArray()) }
    val hardwareInfo = entry.secret.passkey?.hardwareKeyInfo
    PasskeySection(
        hasPasskeyData = SensitiveFieldKey.PASSKEY_PRIVATE_REFERENCE in uiState.sensitiveFieldKeys,
        revealedPasskeyData = passkeyData,
        hardwareKeyInfo = hardwareInfo,
        onPasskeyCopy = { onAction(DetailUiAction.CopyField(FieldKey.PASSKEY_DATA)) },
        onPasskeyReveal = {
            onAction(DetailUiAction.ToggleFieldVisibility(RevealedFieldKey.PASSKEY_DATA))
        },
        onHardwareKeyCopy = { onAction(DetailUiAction.CopyField(FieldKey.HARDWARE_INFO)) },
    )
}
