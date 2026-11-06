package com.aozijx.passly.feature.autofill.presentation.legacy

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.feature.autofill.presentation.legacy.ui.AutofillCandidateBottomSheet

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
        targetAppPackageName = request.packageName,
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
    targetAppPackageName: String,
    onCandidateSelected: (EntryId) -> Unit,
    onCancel: () -> Unit,
) {
    when (displayState) {
        AutofillFillDisplayState.Hidden -> {
            Unit
        }

        is AutofillFillDisplayState.Candidates -> {
            AutofillCandidateBottomSheet(
                candidates = displayState.candidates,
                targetAppPackageName = targetAppPackageName,
                onCandidateSelected = onCandidateSelected,
                onCancel = onCancel,
            )
        }
    }
}
