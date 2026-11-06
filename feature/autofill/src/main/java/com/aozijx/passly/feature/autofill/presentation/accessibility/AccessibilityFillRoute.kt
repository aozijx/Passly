package com.aozijx.passly.feature.autofill.presentation.accessibility

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aozijx.passly.feature.autofill.presentation.legacy.ui.AutofillCandidateBottomSheet

@Composable
internal fun AccessibilityFillRoute(
    viewModel: AccessibilityFillViewModel,
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
        targetAppPackageName = candidateState.targetAppPackageName,
        onCandidateSelected = viewModel::select,
        onCancel = viewModel::cancel,
    )
}
