package com.aozijx.passly.presentation.feature.autofill.legacy

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aozijx.passly.domain.autofill.model.ResolvedCandidate
import com.aozijx.passly.presentation.feature.autofill.legacy.ui.AutofillCandidateBottomSheet
import com.aozijx.passly.presentation.feature.autofill.legacy.ui.AutofillCandidateItem

@Composable
internal fun AutofillFillRoute(
    request: AutofillFillRequest,
    viewModel: AutofillFillViewModel,
    onResult: (AutofillAuthenticationPayload?) -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(request, viewModel) {
        viewModel.onAction(AutofillFillUiAction.Initialize(request))
    }
    LaunchedEffect(state) {
        when (val current = state) {
            is AutofillFillUiState.Result -> onResult(current.payload)
            is AutofillFillUiState.Error -> onResult(null)
            AutofillFillUiState.Initial,
            AutofillFillUiState.Loading,
            is AutofillFillUiState.ShowCandidates -> Unit
        }
    }

    val candidates = (state as? AutofillFillUiState.ShowCandidates)?.candidates ?: return
    val candidatesById = candidates.associateBy { it.entry.id.value }
    AutofillCandidateBottomSheet(
        candidates = candidates.map(ResolvedCandidate::toUiItem),
        onCandidateSelected = { candidateId ->
            candidatesById[candidateId]?.let { candidate ->
                viewModel.onAction(AutofillFillUiAction.CandidateSelected(candidate))
            }
        },
        onCancel = { onResult(null) },
    )
}

private fun ResolvedCandidate.toUiItem() = AutofillCandidateItem(
    id = entry.id.value,
    iconName = entry.profile.icon.name,
    iconCustomPath = entry.profile.icon.customReference,
    associatedAppPackage = entry.profile.associations.applicationIds.firstOrNull(),
    entryTypeKey = entry.entryType.name,
    title = entry.title,
    username = entry.username,
    associatedDomain = entry.associatedDomain,
)
