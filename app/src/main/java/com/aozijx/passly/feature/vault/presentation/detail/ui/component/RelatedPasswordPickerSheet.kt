package com.aozijx.passly.feature.vault.presentation.detail.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aozijx.passly.R
import com.aozijx.passly.feature.vault.presentation.detail.DetailUiAction
import com.aozijx.passly.feature.vault.presentation.detail.ui.model.RelatedPasswordBindingUiState

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun RelatedPasswordPickerSheet(
    state: RelatedPasswordBindingUiState,
    onAction: (DetailUiAction) -> Unit,
) {
    if (state == RelatedPasswordBindingUiState.Closed) return
    var search by remember { mutableStateOf("") }

    ModalBottomSheet(onDismissRequest = { onAction(DetailUiAction.DismissPasswordBinding) }) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(stringResource(R.string.vault_detail_bind_password_title))
            when (val current = state) {
                RelatedPasswordBindingUiState.Closed -> Unit
                RelatedPasswordBindingUiState.Loading ->
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                RelatedPasswordBindingUiState.LoadFailed -> {
                    Text(stringResource(R.string.vault_detail_bind_password_load_failed))
                    TextButton(onClick = { onAction(DetailUiAction.RetryPasswordBinding) }) {
                        Text(stringResource(R.string.retry))
                    }
                }
                is RelatedPasswordBindingUiState.Ready -> {
                    OutlinedTextField(
                        value = search,
                        onValueChange = { search = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(R.string.vault_detail_bind_password_search)) },
                        singleLine = true,
                    )
                    if (current.bindFailed) {
                        Text(stringResource(R.string.vault_detail_bind_password_failed))
                    }
                    val candidates = current.candidates.filter { candidate ->
                        candidate.title.contains(search, ignoreCase = true) ||
                            candidate.username.contains(search, ignoreCase = true)
                    }
                    if (current.candidates.isEmpty()) {
                        Text(stringResource(R.string.vault_detail_bind_password_empty))
                    } else if (candidates.isEmpty()) {
                        Text(stringResource(R.string.vault_detail_bind_password_no_matches))
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 480.dp)) {
                            items(candidates, key = { it.id }) { candidate ->
                                ListItem(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ListItemDefaults.colors(),
                                    content = {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(candidate.title)
                                                if (candidate.username.isNotBlank()) Text(candidate.username)
                                            }
                                            if (current.savingEntryId == candidate.id) {
                                                CircularProgressIndicator()
                                            } else {
                                                TextButton(
                                                    enabled = current.savingEntryId == null,
                                                    onClick = { onAction(DetailUiAction.BindPassword(candidate.id)) },
                                                ) {
                                                    Text(stringResource(R.string.vault_detail_bind_password_action))
                                                }
                                            }
                                        }
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
