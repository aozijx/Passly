package com.aozijx.passly.presentation.feature.vault.detail.component

import androidx.compose.runtime.Composable
import com.aozijx.passly.domain.entry.model.Entry
import com.aozijx.passly.domain.entry.model.FieldKey
import com.aozijx.passly.domain.entry.model.sensitive.SensitiveFieldKey
import com.aozijx.passly.presentation.feature.vault.detail.DetailSectionActionHandler
import com.aozijx.passly.presentation.feature.vault.detail.DetailUiAction
import com.aozijx.passly.presentation.feature.vault.detail.DetailUiState
import com.aozijx.passly.presentation.feature.vault.detail.RevealedFieldKey
import com.aozijx.passly.presentation.ui.vault.detail.component.PasskeySection

@Composable
internal fun DetailPasskeyHost(
    entry: Entry,
    uiState: DetailUiState,
    onAction: (DetailUiAction) -> Unit,
) {
    val handler = DetailSectionActionHandler(onAction)
    val passkeyData = uiState.revealed(RevealedFieldKey.PASSKEY_DATA)
        ?.let { String(it.toCharArray()) }
    val hardwareInfo = entry.secret.passkey?.hardwareKeyInfo
    PasskeySection(
        hasPasskeyData = SensitiveFieldKey.PASSKEY_PRIVATE_REFERENCE in uiState.sensitiveFieldKeys,
        revealedPasskeyData = passkeyData,
        hardwareKeyInfo = hardwareInfo,
        onPasskeyCopy = { handler.copy(FieldKey.PASSKEY_DATA) },
        onPasskeyReveal = {
            if (passkeyData != null) {
                onAction(DetailUiAction.RevealField(RevealedFieldKey.PASSKEY_DATA, null))
            } else {
                onAction(DetailUiAction.RevealHighSensitivityField(RevealedFieldKey.PASSKEY_DATA))
            }
        },
        onHardwareKeyCopy = { handler.copy(FieldKey.HARDWARE_INFO) },
    )
}
