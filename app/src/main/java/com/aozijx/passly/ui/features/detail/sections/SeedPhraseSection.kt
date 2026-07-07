package com.aozijx.passly.ui.features.detail.sections

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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.aozijx.passly.core.auth.biometric.BiometricPromptLauncher
import com.aozijx.passly.core.platform.ClipboardUtils
import com.aozijx.passly.domain.model.VaultEntry
import com.aozijx.passly.domain.model.VaultHistory
import com.aozijx.passly.ui.features.detail.components.DetailItem
import com.aozijx.passly.ui.features.detail.contract.DetailEvent
import com.aozijx.passly.ui.features.detail.internal.DetailSectionActionHandler
import com.aozijx.passly.ui.features.detail.internal.copySensitiveField
import com.aozijx.passly.ui.features.detail.internal.toggleRevealSensitiveField

@Composable
fun SeedPhraseSection(
    launcher: BiometricPromptLauncher,
    entry: VaultEntry,
    revealedSeedPhrase: String?,
    onSeedPhraseRevealed: (String?) -> Unit,
    onAuthenticate: (launcher: BiometricPromptLauncher, title: String, subtitle: String, onSuccess: () -> Unit) -> Unit,
    onEvent: (DetailEvent) -> Unit
) {
    val context = LocalContext.current
    val seedPhraseCopiedMsg = stringResource(R.string.seed_phrase_copied)
    val actionHandler = DetailSectionActionHandler(
        launcher = launcher,
        onAuthenticate = onAuthenticate,
        onEvent = onEvent
    )

    val wordList = remember(revealedSeedPhrase) {
        revealedSeedPhrase?.split(" ")?.filter { it.isNotBlank() } ?: emptyList()
    }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        DetailItem(
            label = stringResource(R.string.seed_phrase_title),
            value = if (revealedSeedPhrase != null) stringResource(R.string.seed_phrase_revealed) else stringResource(
                R.string.hidden_mask
            ),
            isRevealed = revealedSeedPhrase != null,
            onCopy = {
                copySensitiveField(
                    context = context,
                    handler = actionHandler,
                    fieldName = "seed phrase",
                    revealedValue = revealedSeedPhrase,
                    sourceValue = entry.cryptoSeedPhrase,
                    authTitle = "解密助记词",
                    authSubtitle = "验证身份以复制助记词",
                    onReveal = onSeedPhraseRevealed,
                    afterCopy = {
                        Toast.makeText(context, seedPhraseCopiedMsg, Toast.LENGTH_SHORT).show()
                    }
                )
            },
            onEdit = {
                toggleRevealSensitiveField(
                    handler = actionHandler,
                    fieldName = "seed phrase",
                    revealedValue = revealedSeedPhrase,
                    sourceValue = entry.cryptoSeedPhrase,
                    authTitle = "解密助记词",
                    authSubtitle = "验证身份以查看助记词",
                    onReveal = onSeedPhraseRevealed
                )
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

        if (revealedSeedPhrase == null) {
            Button(
                onClick = {
                    toggleRevealSensitiveField(
                        handler = actionHandler,
                        fieldName = "seed phrase",
                        revealedValue = revealedSeedPhrase,
                        sourceValue = entry.cryptoSeedPhrase,
                        authTitle = "解密助记词",
                        authSubtitle = "验证身份以显示助记词",
                        onReveal = onSeedPhraseRevealed
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.Visibility, null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.vault_reveal_info))
            }
        }

        if (entry.notes != null) {
            DetailItem(
                label = stringResource(R.string.vault_detail_notes),
                value = entry.notes,
                isRevealed = true,
                onCopy = {
                    ClipboardUtils.copy(context, entry.notes)
                    actionHandler.record("notes", VaultHistory.HistoryType.COPY)
                },
                onEdit = {}
            )
        }
    }
}

@Composable
private fun SeedWordChip(index: Int, word: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        shape = RoundedCornerShape(8.dp)
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