package com.aozijx.passly.features.detail.sections

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.aozijx.passly.R
import com.aozijx.passly.core.platform.ClipboardUtils
import com.aozijx.passly.domain.model.core.VaultEntry
import com.aozijx.passly.features.detail.components.DetailItem
import com.aozijx.passly.features.detail.internal.EntryEditState
import com.aozijx.passly.features.main.MainViewModel
import com.aozijx.passly.features.vault.VaultViewModel

@Composable
fun SeedPhraseSection(
    activity: FragmentActivity,
    entry: VaultEntry,
    editState: EntryEditState,
    revealedPassword: String?,
    onPasswordRevealed: (String?) -> Unit,
    vaultViewModel: VaultViewModel,
    mainViewModel: MainViewModel,
    onEntryUpdated: (VaultEntry) -> Unit
) {
    val context = LocalContext.current
    val seedPhraseLabel = stringResource(R.string.seed_phrase)
    val seedPhraseCopiedMsg = stringResource(R.string.seed_phrase_copied)

    var revealedSeedPhrase by remember { mutableStateOf<String?>(null) }
    var wordList by remember { mutableStateOf<List<String>>(emptyList()) }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        DetailItem(
            label = stringResource(R.string.seed_phrase_title),
            value = if (revealedSeedPhrase != null) stringResource(R.string.seed_phrase_revealed) else stringResource(R.string.label_hidden_mask),
            isRevealed = revealedSeedPhrase != null,
            onCopy = {
                val seedPhrase = revealedSeedPhrase
                if (seedPhrase != null) {
                    ClipboardUtils.copy(context, seedPhrase)
                    Toast.makeText(context, seedPhraseCopiedMsg, Toast.LENGTH_SHORT).show()
                } else {
                    val encryptedSeedPhrase = entry.cryptoSeedPhrase
                    if (encryptedSeedPhrase != null) {
                        vaultViewModel.decryptSingle(activity, encryptedSeedPhrase, mainViewModel::authenticate) { decrypted ->
                            decrypted?.let {
                                ClipboardUtils.copy(context, it)
                                Toast.makeText(context, seedPhraseCopiedMsg, Toast.LENGTH_SHORT).show()
                                revealedSeedPhrase = it
                                wordList = it.split(" ").filter { word -> word.isNotBlank() }
                            }
                        }
                    }
                }
            },
            onEdit = {
                if (revealedSeedPhrase != null) {
                    revealedSeedPhrase = null
                    wordList = emptyList()
                } else {
                    val seedPhrase = entry.cryptoSeedPhrase
                    if (seedPhrase != null) {
                        vaultViewModel.decryptSingle(activity, seedPhrase, mainViewModel::authenticate) { decrypted ->
                            decrypted?.let {
                                revealedSeedPhrase = it
                                wordList = it.split(" ").filter { word -> word.isNotBlank() }
                            }
                        }
                    }
                }
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
                    val encryptedSeedPhrase = entry.cryptoSeedPhrase
                    if (encryptedSeedPhrase != null) {
                        vaultViewModel.decryptSingle(activity, encryptedSeedPhrase, mainViewModel::authenticate) { decrypted ->
                            decrypted?.let {
                                revealedSeedPhrase = it
                                wordList = it.split(" ").filter { word -> word.isNotBlank() }
                            }
                        }
                    }
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
                onCopy = { entry.notes.let { ClipboardUtils.copy(context, it) } },
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