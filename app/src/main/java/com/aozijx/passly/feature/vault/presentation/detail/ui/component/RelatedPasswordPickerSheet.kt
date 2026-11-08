package com.aozijx.passly.feature.vault.presentation.detail.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aozijx.passly.R
import com.aozijx.passly.feature.vault.presentation.detail.DetailUiAction
import com.aozijx.passly.feature.vault.presentation.detail.RelatedPasswordBindingUiState
import com.aozijx.passly.feature.vault.presentation.detail.RelatedPasswordCandidateSearchResults

@Composable
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
fun RelatedPasswordPickerSheet(
    state: RelatedPasswordBindingUiState,
    onAction: (DetailUiAction) -> Unit,
) {
    if (state == RelatedPasswordBindingUiState.Closed) return
    ModalBottomSheet(onDismissRequest = { onAction(DetailUiAction.DismissPasswordBinding) }) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(stringResource(R.string.vault_detail_bind_password_title))
            when (val current = state) {
                RelatedPasswordBindingUiState.Closed -> Unit
                RelatedPasswordBindingUiState.Loading ->
                    LoadingIndicator(
                        modifier = Modifier.align(Alignment.CenterHorizontally).size(32.dp),
                    )
                RelatedPasswordBindingUiState.LoadFailed -> {
                    Text(stringResource(R.string.vault_detail_bind_password_load_failed))
                    TextButton(onClick = { onAction(DetailUiAction.RetryPasswordBinding) }) {
                        Text(stringResource(R.string.retry))
                    }
                }
                is RelatedPasswordBindingUiState.Ready -> {
                    OutlinedTextField(
                        value = current.query,
                        onValueChange = { onAction(DetailUiAction.PasswordBindingSearchChanged(it)) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(R.string.vault_detail_bind_password_search)) },
                        singleLine = true,
                    )
                    if (current.bindFailed) {
                        Text(stringResource(R.string.vault_detail_bind_password_failed))
                    }
                    when (val results = current.results) {
                        RelatedPasswordCandidateSearchResults.NoCandidates ->
                            Text(stringResource(R.string.vault_detail_bind_password_empty))
                        RelatedPasswordCandidateSearchResults.NoMatches ->
                            Text(stringResource(R.string.vault_detail_bind_password_no_matches))
                        is RelatedPasswordCandidateSearchResults.Matches -> {
                            LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 480.dp)) {
                                items(results.candidates, key = { it.id }) { candidate ->
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
                                                    candidate.username?.let { Text(it) }
                                                }
                                                if (current.savingEntryId == candidate.id) {
                                                    LoadingIndicator(
                                                        modifier = Modifier.size(24.dp).testTag("related-password-saving"),
                                                    )
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
}
