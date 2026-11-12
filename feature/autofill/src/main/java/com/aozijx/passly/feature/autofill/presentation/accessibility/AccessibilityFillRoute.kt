package com.aozijx.passly.feature.autofill.presentation.accessibility

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aozijx.passly.domain.entry.model.query.EntryListItem
import com.aozijx.passly.feature.autofill.presentation.legacy.ui.AutofillCandidateBottomSheet

@Composable
internal fun AccessibilityFillRoute(
    viewModel: AccessibilityFillViewModel,
    entryIcon: @Composable (EntryListItem, Modifier) -> Unit,
    onComplete: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.start() }
    LaunchedEffect(state) {
        if (state == AccessibilityFillUiState.Done) onComplete()
    }

    val candidateState = state as? AccessibilityFillUiState.Candidates ?: return
    AutofillCandidateBottomSheet(
        candidates = candidateState.values,
        entryIcon = entryIcon,
        onCandidateSelected = viewModel::select,
        onCancel = viewModel::cancel,
    )
}
