package com.aozijx.passly.presentation.feature.vault.detail.section

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aozijx.passly.R
import com.aozijx.passly.presentation.feature.vault.detail.DetailAuthenticate
import com.aozijx.passly.presentation.feature.vault.detail.DetailUiAction
import com.aozijx.passly.presentation.feature.vault.detail.RevealedFieldKey
import com.aozijx.passly.presentation.feature.vault.detail.DetailSectionActionHandler
import com.aozijx.passly.presentation.feature.vault.detail.copySensitiveField
import com.aozijx.passly.presentation.feature.vault.detail.component.DetailItem
import com.aozijx.passly.domain.sensitive.OwnedChars

@Composable
fun SeedPhraseSection(
    hasSeedPhrase: Boolean,
    revealedSeedPhrase: String?,
    onSeedPhraseRevealed: (String?) -> Unit,
    onAuthenticate: DetailAuthenticate,
    onAction: (DetailUiAction) -> Unit
) {
    val context = LocalContext.current
    val msgCopySuccess = stringResource(R.string.field_copy_success_message)
    val seedPhraseLabel = stringResource(R.string.seed_phrase)
    val actionHandler = DetailSectionActionHandler(
        onAuthenticate = onAuthenticate,
        onAction = onAction
    )

    val wordList = remember(revealedSeedPhrase) {
        revealedSeedPhrase?.split(" ")?.filter { it.isNotBlank() } ?: emptyList()
    }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        DetailItem(
            label = stringResource(R.string.seed_phrase_title),
            value = revealedSeedPhrase?.let { stringResource(R.string.seed_phrase_revealed) },
            isRevealed = revealedSeedPhrase != null,
            onCopy = {
                copySensitiveField(
                    context = context,
                    handler = actionHandler,
                    fieldName = "seed phrase",
                    revealedValue = revealedSeedPhrase?.let { OwnedChars.fromString(it) },
                    sourceValue = null,
                    afterCopy = {
                        Toast.makeText(
                            context,
                            msgCopySuccess.format(seedPhraseLabel),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                )
            },
            onEdit = null,
            onReveal = {
                if (revealedSeedPhrase != null) onSeedPhraseRevealed(null)
                else onAction(DetailUiAction.RevealHighSensitivityField(RevealedFieldKey.SEED_PHRASE))
            }
        )

        if (wordList.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                shape = MaterialTheme.shapes.large
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.word_index_hint),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        itemsIndexed(wordList) { index, word ->
                            SeedWordChip(index = index + 1, word = word)
                        }
                    }
                }
            }
        }

        if (hasSeedPhrase && revealedSeedPhrase == null) {
            Button(
                onClick = {
                    onAction(DetailUiAction.RevealHighSensitivityField(RevealedFieldKey.SEED_PHRASE))
                },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium
            ) {
                Icon(Icons.Default.Visibility, null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.vault_reveal_info))
            }
        }
    }
}

@Composable
private fun SeedWordChip(index: Int, word: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$index.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f),
                modifier = Modifier.padding(end = 4.dp)
            )
            Text(
                text = word,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}
