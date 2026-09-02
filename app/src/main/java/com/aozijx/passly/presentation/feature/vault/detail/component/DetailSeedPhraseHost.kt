package com.aozijx.passly.presentation.feature.vault.detail.component

import androidx.compose.runtime.Composable
import com.aozijx.passly.domain.entry.model.sensitive.SensitiveFieldKey
import com.aozijx.passly.domain.sensitive.OwnedChars
import com.aozijx.passly.presentation.feature.vault.detail.DetailAuthenticate
import com.aozijx.passly.presentation.feature.vault.detail.DetailSectionActionHandler
import com.aozijx.passly.presentation.feature.vault.detail.DetailUiAction
import com.aozijx.passly.presentation.feature.vault.detail.DetailUiState
import com.aozijx.passly.presentation.feature.vault.detail.RevealedFieldKey
import com.aozijx.passly.presentation.feature.vault.detail.copySensitiveField
import com.aozijx.passly.presentation.ui.vault.detail.component.SeedPhraseSection

@Composable
internal fun DetailSeedPhraseHost(
    uiState: DetailUiState,
    onAction: (DetailUiAction) -> Unit,
    onAuthenticate: DetailAuthenticate,
    onCopySensitive: (String) -> Unit,
) {
    val handler = DetailSectionActionHandler(onAuthenticate, onAction, onCopySensitive)
    val seed = uiState.revealed(RevealedFieldKey.SEED_PHRASE)?.let { String(it.toCharArray()) }
    SeedPhraseSection(
        hasSeedPhrase = SensitiveFieldKey.SEED_PHRASE in uiState.sensitiveFieldKeys,
        revealedSeedPhrase = seed,
        onCopy = {
            copySensitiveField(
                handler,
                "seed phrase",
                seed?.let(OwnedChars::fromString),
                null,
            )
        },
        onReveal = {
            if (seed != null) {
                onAction(DetailUiAction.RevealField(RevealedFieldKey.SEED_PHRASE, null))
            } else {
                onAction(DetailUiAction.RevealHighSensitivityField(RevealedFieldKey.SEED_PHRASE))
            }
        },
    )
}
