package com.aozijx.passly.presentation.feature.vault.detail.binding

import androidx.compose.runtime.Composable
import com.aozijx.passly.domain.entry.model.FieldKey
import com.aozijx.passly.domain.entry.model.sensitive.SensitiveFieldKey
import com.aozijx.passly.presentation.feature.vault.detail.DetailSectionActionHandler
import com.aozijx.passly.presentation.feature.vault.detail.DetailUiAction
import com.aozijx.passly.presentation.feature.vault.detail.DetailUiState
import com.aozijx.passly.presentation.feature.vault.detail.RevealedFieldKey
import com.aozijx.passly.presentation.ui.vault.detail.component.SeedPhraseSection

@Composable
internal fun DetailSeedPhraseBinding(
    uiState: DetailUiState,
    onAction: (DetailUiAction) -> Unit,
) {
    val handler = DetailSectionActionHandler(onAction)
    val seed = uiState.revealed(RevealedFieldKey.SEED_PHRASE)?.let { String(it.toCharArray()) }
    SeedPhraseSection(
        hasSeedPhrase = SensitiveFieldKey.SEED_PHRASE in uiState.sensitiveFieldKeys,
        revealedSeedPhrase = seed,
        onCopy = { handler.copy(FieldKey.SEED_PHRASE) },
        onReveal = {
            if (seed != null) {
                onAction(DetailUiAction.RevealField(RevealedFieldKey.SEED_PHRASE, null))
            } else {
                onAction(DetailUiAction.RevealHighSensitivityField(RevealedFieldKey.SEED_PHRASE))
            }
        },
    )
}
