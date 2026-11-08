package com.aozijx.passly.feature.vault.presentation.editor.bankcard

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aozijx.passly.R
import com.aozijx.passly.feature.vault.entry.EntryEditorSeedConsumeResult
import com.aozijx.passly.feature.vault.presentation.editor.EditorLoadFailureEffectHandler
import com.aozijx.passly.feature.vault.presentation.editor.EditorSaveEffectHandler
import com.aozijx.passly.feature.vault.presentation.editor.ui.bankcard.AddBankCardEditorScreen
import com.aozijx.passly.feature.vault.presentation.editor.ui.common.rememberAddEntryFabTransitionModifier

@Composable
fun AddBankCardEditorRoute(
    onBack: () -> Unit,
    onSaved: () -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    editSeedToken: String? = null,
    consumeEditSeed: ((String) -> EntryEditorSeedConsumeResult)? = null,
) {
    val viewModel = hiltViewModel<AddBankCardViewModel>()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val saveFailedMessage = stringResource(R.string.vault_add_bank_card_save_failed)
    val loadFailedMessage = stringResource(R.string.vault_edit_entry_load_failed)
    val saveActionModifier =
        Modifier.rememberAddEntryFabTransitionModifier(
            sharedTransitionScope = sharedTransitionScope,
            animatedVisibilityScope = animatedVisibilityScope,
        )
    EditorSaveEffectHandler(viewModel.effects, snackbarHostState, saveFailedMessage, onSaved)
    EditorLoadFailureEffectHandler(uiState.loadPhase, editSeedToken != null, snackbarHostState, loadFailedMessage)
    LaunchedEffect(viewModel, editSeedToken) {
        editSeedToken?.let { token ->
            viewModel.loadExistingEntry(consumeEditSeed?.invoke(token) ?: EntryEditorSeedConsumeResult.Missing)
        }
    }
    AddBankCardEditorScreen(
        state = uiState,
        onAction = viewModel::onAction,
        onBack = onBack,
        snackbarHostState = snackbarHostState,
        saveActionModifier = saveActionModifier,
        isEditingExisting = editSeedToken != null,
    )
}
