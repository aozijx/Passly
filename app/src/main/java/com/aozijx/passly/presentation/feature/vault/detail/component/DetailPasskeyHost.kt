package com.aozijx.passly.presentation.feature.vault.detail.component

import androidx.compose.runtime.Composable
import com.aozijx.passly.domain.entry.model.Entry
import com.aozijx.passly.domain.entry.model.activity.ActivityType
import com.aozijx.passly.domain.entry.model.sensitive.SensitiveFieldKey
import com.aozijx.passly.domain.sensitive.OwnedChars
import com.aozijx.passly.presentation.feature.vault.detail.DetailAuthenticate
import com.aozijx.passly.presentation.feature.vault.detail.DetailSectionActionHandler
import com.aozijx.passly.presentation.feature.vault.detail.DetailUiAction
import com.aozijx.passly.presentation.feature.vault.detail.DetailUiState
import com.aozijx.passly.presentation.feature.vault.detail.RevealedFieldKey
import com.aozijx.passly.presentation.feature.vault.detail.copySensitiveField
import com.aozijx.passly.presentation.ui.vault.detail.component.PasskeySection

@Composable
internal fun DetailPasskeyHost(
    entry: Entry,
    uiState: DetailUiState,
    onAction: (DetailUiAction) -> Unit,
    onAuthenticate: DetailAuthenticate,
    onCopySensitive: (String) -> Unit,
) {
    val handler = DetailSectionActionHandler(onAuthenticate, onAction, onCopySensitive)
    val passkeyData = uiState.revealed(RevealedFieldKey.PASSKEY_DATA)
        ?.let { String(it.toCharArray()) }
    val hardwareInfo = entry.secret.passkey?.hardwareKeyInfo
    PasskeySection(
        hasPasskeyData = SensitiveFieldKey.PASSKEY_PRIVATE_REFERENCE in uiState.sensitiveFieldKeys,
        revealedPasskeyData = passkeyData,
        hardwareKeyInfo = hardwareInfo,
        onPasskeyCopy = {
            copySensitiveField(
                handler,
                "passkey data",
                passkeyData?.let(OwnedChars::fromString),
                null,
            )
        },
        onPasskeyReveal = {
            if (passkeyData != null) {
                onAction(DetailUiAction.RevealField(RevealedFieldKey.PASSKEY_DATA, null))
            } else {
                onAction(DetailUiAction.RevealHighSensitivityField(RevealedFieldKey.PASSKEY_DATA))
            }
        },
        onHardwareKeyCopy = {
            hardwareInfo?.let {
                handler.onCopySensitive(it)
                handler.record("hardware key info", ActivityType.COPY_PASSWORD)
            }
        },
    )
}
