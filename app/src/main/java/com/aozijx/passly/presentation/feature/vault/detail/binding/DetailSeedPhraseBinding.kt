package com.aozijx.passly.presentation.feature.vault.detail.binding

import androidx.compose.runtime.Composable
import com.aozijx.passly.domain.entry.model.FieldKey
import com.aozijx.passly.domain.entry.model.sensitive.SensitiveFieldKey
import com.aozijx.passly.presentation.feature.vault.detail.DetailUiAction
import com.aozijx.passly.presentation.feature.vault.detail.DetailUiState
import com.aozijx.passly.presentation.feature.vault.detail.RevealedFieldKey
import com.aozijx.passly.presentation.ui.vault.detail.component.SeedPhraseSection

@Composable
internal fun DetailSeedPhraseBinding(
    uiState: DetailUiState,
    onAction: (DetailUiAction) -> Unit,
) {
    val seed = uiState.revealed(RevealedFieldKey.SEED_PHRASE)?.let { String(it.toCharArray()) }
    SeedPhraseSection(
        hasSeedPhrase = SensitiveFieldKey.SEED_PHRASE in uiState.sensitiveFieldKeys,
        revealedSeedPhrase = seed,
        onCopy = { onAction(DetailUiAction.CopyField(FieldKey.SEED_PHRASE)) },
        onReveal = {
            onAction(DetailUiAction.ToggleFieldVisibility(RevealedFieldKey.SEED_PHRASE))
        },
    )
}
