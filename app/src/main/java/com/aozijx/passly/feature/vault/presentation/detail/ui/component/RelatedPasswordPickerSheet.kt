package com.aozijx.passly.feature.vault.presentation.detail.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.aozijx.passly.R
import com.aozijx.passly.feature.vault.presentation.common.ui.SheetTitleSearchHeader
import com.aozijx.passly.feature.vault.presentation.detail.DetailUiAction
import com.aozijx.passly.feature.vault.presentation.detail.RelatedPasswordBindingOperationState
import com.aozijx.passly.feature.vault.presentation.detail.RelatedPasswordBindingUiState
import com.aozijx.passly.feature.vault.presentation.detail.RelatedPasswordCandidateSearchResults
import com.aozijx.passly.feature.vault.presentation.detail.RelatedPasswordCandidateUsernameUiState

@Composable
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
fun RelatedPasswordPickerSheet(
    state: RelatedPasswordBindingUiState,
    onAction: (DetailUiAction) -> Unit,
) {
    if (state == RelatedPasswordBindingUiState.Closed) return
    val sheetState =
        rememberBottomSheetState(
            initialValue = SheetValue.Hidden,
            enabledValues =
                setOf(
                    SheetValue.Hidden,
                    SheetValue.PartiallyExpanded,
                    SheetValue.Expanded,
                ),
        )
    ModalBottomSheet(
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        onDismissRequest = { onAction(DetailUiAction.DismissPasswordBinding) },
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        ) {
            item {
                Box(Modifier.padding(bottom = 12.dp)) {
                    SheetTitleSearchHeader(
                        title = stringResource(R.string.vault_detail_bind_password_title),
                        searchLabel = stringResource(R.string.vault_detail_bind_password_search),
                        query = state.query,
                        onQueryChanged = { onAction(DetailUiAction.PasswordBindingSearchChanged(it)) },
                        searchEnabled = state is RelatedPasswordBindingUiState.Ready,
                    )
                }
            }
            when (val current = state) {
                RelatedPasswordBindingUiState.Closed -> {
                    Unit
                }

                RelatedPasswordBindingUiState.Loading -> {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center,
                        ) {
                            LoadingIndicator(modifier = Modifier.size(48.dp))
                        }
                    }
                }

                RelatedPasswordBindingUiState.LoadFailed -> {
                    item {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(stringResource(R.string.vault_detail_bind_password_load_failed))
                            TextButton(onClick = { onAction(DetailUiAction.RetryPasswordBinding) }) {
                                Text(stringResource(R.string.retry))
                            }
                        }
                    }
                }

                is RelatedPasswordBindingUiState.Ready -> {
                    if (current.operation == RelatedPasswordBindingOperationState.Failed) {
                        item {
                            Text(
                                text = stringResource(R.string.vault_detail_bind_password_failed),
                                modifier = Modifier.padding(bottom = 8.dp),
                            )
                        }
                    }
                    when (val results = current.results) {
                        RelatedPasswordCandidateSearchResults.NoCandidates -> {
                            item {
                                Text(
                                    text = stringResource(R.string.vault_detail_bind_password_empty),
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.Center,
                                )
                            }
                        }

                        RelatedPasswordCandidateSearchResults.NoMatches -> {
                            item {
                                Text(
                                    text = stringResource(R.string.vault_detail_bind_password_no_matches),
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.Center,
                                )
                            }
                        }

                        is RelatedPasswordCandidateSearchResults.Matches -> {
                            items(results.candidates, key = { it.id }) { candidate ->
                                Row(
                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp)
                                            .clip(MaterialTheme.shapes.large)
                                            .clickable(
                                                enabled = current.operation !is RelatedPasswordBindingOperationState.Saving,
                                                role = Role.Button,
                                            ) {
                                                onAction(DetailUiAction.BindPassword(candidate.id))
                                            }.padding(16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Link,
                                        contentDescription = null,
                                        modifier = Modifier.size(40.dp),
                                        tint = MaterialTheme.colorScheme.primary,
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = candidate.title,
                                            style = MaterialTheme.typography.titleMedium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                        when (val username = candidate.username) {
                                            RelatedPasswordCandidateUsernameUiState.Hidden -> {
                                                Unit
                                            }

                                            is RelatedPasswordCandidateUsernameUiState.Visible -> {
                                                Text(
                                                    text = username.value,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                )
                                            }
                                        }
                                    }
                                    if (current.operation == RelatedPasswordBindingOperationState.Saving(candidate.id)) {
                                        LoadingIndicator(
                                            modifier = Modifier.size(24.dp).testTag("related-password-saving"),
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.ChevronRight,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
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
}
