package com.aozijx.passly.presentation.feature.autofill.legacy

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aozijx.passly.domain.autofill.model.AutofillCandidateSummary
import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.presentation.feature.autofill.legacy.ui.AutofillCandidateBottomSheet
import com.aozijx.passly.presentation.feature.autofill.legacy.ui.AutofillCandidateItem
import com.aozijx.passly.presentation.mapping.entry.toUiModel

@Composable
internal fun AutofillFillRoute(
    request: AutofillFillRequest,
    viewModel: AutofillFillViewModel,
    onResult: (AutofillFillResult) -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(request, viewModel) {
        viewModel.onAction(AutofillFillUiAction.Initialize(request))
    }
    AutofillCompletionEffect(state, onResult)
    AutofillFillContent(
        displayState = state.toDisplayState(),
        onCandidateSelected = { entryId ->
            viewModel.onAction(AutofillFillUiAction.CandidateSelected(entryId))
        },
        onCancel = { viewModel.onAction(AutofillFillUiAction.Cancel) },
    )
}

@Composable
private fun AutofillCompletionEffect(
    state: AutofillFillUiState,
    onResult: (AutofillFillResult) -> Unit,
) {
    LaunchedEffect(state) {
        when (state) {
            is AutofillFillUiState.Result -> onResult(state.result)
            is AutofillFillUiState.Error -> onResult(AutofillFillResult.Cancelled)
            AutofillFillUiState.Initial,
            AutofillFillUiState.Loading,
            is AutofillFillUiState.ShowCandidates,
            -> Unit
        }
    }
}

@Composable
private fun AutofillFillContent(
    displayState: AutofillFillDisplayState,
    onCandidateSelected: (EntryId) -> Unit,
    onCancel: () -> Unit,
) {
    when (displayState) {
        AutofillFillDisplayState.Hidden -> Unit
        is AutofillFillDisplayState.Candidates -> AutofillCandidateBottomSheet(
            candidates = displayState.candidates.map(AutofillCandidateSummary::toUiItem),
            onCandidateSelected = onCandidateSelected,
            onCancel = onCancel,
        )
    }
}

private fun AutofillCandidateSummary.toUiItem() = AutofillCandidateItem(
    id = entry.id,
    iconName = entry.profile.icon.name,
    iconCustomPath = entry.profile.icon.customReference,
    associatedAppPackage = entry.profile.associations.applicationIds.firstOrNull(),
    entryType = entry.entryType.toUiModel(),
    title = entry.title,
    username = entry.username,
    associatedDomain = entry.associatedDomain,
)
