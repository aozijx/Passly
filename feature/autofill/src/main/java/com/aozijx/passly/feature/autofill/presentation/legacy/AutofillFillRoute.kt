package com.aozijx.passly.feature.autofill.presentation.legacy

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.feature.autofill.presentation.legacy.ui.AutofillCandidateBottomSheet
import com.aozijx.passly.feature.autofill.presentation.legacy.ui.AutofillWebConfirmationDialog

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
        state = state,
        targetAppPackageName = request.packageName,
        onCandidateSelected = { entryId ->
            viewModel.onAction(AutofillFillUiAction.CandidateSelected(entryId))
        },
        onConfirmWebRecipient = { viewModel.onAction(AutofillFillUiAction.ConfirmWebRecipient) },
        onConfirmWebEntry = { viewModel.onAction(AutofillFillUiAction.ConfirmWebEntry) },
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
            is AutofillFillUiState.ConfirmWebRecipient,
            is AutofillFillUiState.ConfirmWebEntry,
            -> Unit
        }
    }
}

@Composable
private fun AutofillFillContent(
    state: AutofillFillUiState,
    targetAppPackageName: String,
    onCandidateSelected: (EntryId) -> Unit,
    onConfirmWebRecipient: () -> Unit,
    onConfirmWebEntry: () -> Unit,
    onCancel: () -> Unit,
) {
    when (state) {
        AutofillFillUiState.Initial,
        AutofillFillUiState.Loading,
        is AutofillFillUiState.Result,
        is AutofillFillUiState.Error,
        -> {
            Unit
        }

        is AutofillFillUiState.ShowCandidates -> {
            AutofillCandidateBottomSheet(
                candidates = state.candidates,
                targetAppPackageName = targetAppPackageName,
                onCandidateSelected = onCandidateSelected,
                onCancel = onCancel,
            )
        }

        is AutofillFillUiState.ConfirmWebRecipient -> {
            AutofillWebConfirmationDialog(
                packageName = state.packageName,
                webDomain = state.webDomain,
                onConfirm = onConfirmWebRecipient,
                onCancel = onCancel,
            )
        }

        is AutofillFillUiState.ConfirmWebEntry -> {
            AutofillWebConfirmationDialog(
                packageName = state.packageName,
                webDomain = state.webDomain,
                candidate = state.candidate,
                onConfirm = onConfirmWebEntry,
                onCancel = onCancel,
            )
        }
    }
}
