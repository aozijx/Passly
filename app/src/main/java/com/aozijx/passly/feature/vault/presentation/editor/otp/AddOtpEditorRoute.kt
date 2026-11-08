package com.aozijx.passly.feature.vault.presentation.editor.otp

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aozijx.passly.R
import com.aozijx.passly.domain.entry.otp.OtpAuthUriCodec
import com.aozijx.passly.domain.entry.otp.OtpUriParseResult
import com.aozijx.passly.feature.scanner.presentation.ScannerRoute
import com.aozijx.passly.feature.scanner.presentation.ScannerResultHandling
import com.aozijx.passly.feature.scanner.presentation.ScannerPrimaryActionLabel
import com.aozijx.passly.feature.scanner.presentation.ScannerResultPresentation
import com.aozijx.passly.feature.scanner.presentation.ScannerResultPlan
import com.aozijx.passly.feature.vault.entry.EntryEditorSeedConsumeResult
import com.aozijx.passly.feature.vault.presentation.editor.EditorLoadFailureEffectHandler
import com.aozijx.passly.feature.vault.presentation.editor.EditorSaveEffectHandler
import com.aozijx.passly.feature.vault.presentation.editor.ui.common.rememberAddEntryFabTransitionModifier
import com.aozijx.passly.feature.vault.presentation.editor.ui.otp.AddOtpEditorScreen
import com.aozijx.passly.presentation.shared.event.collectPageEffects

@Composable
fun AddOtpEditorRoute(
    onBack: () -> Unit,
    onSaved: () -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    editSeedToken: String? = null,
    consumeEditSeed: ((String) -> EntryEditorSeedConsumeResult)? = null,
) {
    val viewModel = hiltViewModel<AddOtpViewModel>()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val saveFailedMessage = stringResource(R.string.vault_add_otp_save_failed)
    val loadFailedMessage = stringResource(R.string.vault_edit_entry_load_failed)
    val uriParsedMessage = stringResource(R.string.vault_otp_uri_parsed)
    val uriParseFailedMessage = stringResource(R.string.vault_otp_uri_parse_failed)
    val saveActionModifier =
        Modifier.rememberAddEntryFabTransitionModifier(
            sharedTransitionScope = sharedTransitionScope,
            animatedVisibilityScope = animatedVisibilityScope,
        )
    var overlay by remember { mutableStateOf<OtpEditorOverlay>(OtpEditorOverlay.None) }

    DisposableEffect(viewModel) {
        val owner = viewModel.onPageEntered()
        onDispose { viewModel.onPageExited(owner) }
    }

    EditorSaveEffectHandler(viewModel.effects, snackbarHostState, saveFailedMessage, onSaved)
    EditorLoadFailureEffectHandler(uiState.loadPhase, editSeedToken != null, snackbarHostState, loadFailedMessage)
    LaunchedEffect(viewModel, editSeedToken) {
        editSeedToken?.let { token ->
            viewModel.loadExistingEntry(consumeEditSeed?.invoke(token) ?: EntryEditorSeedConsumeResult.Missing)
        }
    }
    LaunchedEffect(
        viewModel,
        context,
        snackbarHostState,
        uriParsedMessage,
        uriParseFailedMessage,
    ) {
        collectPageEffects(lifecycleOwner.lifecycle, viewModel.events) { event ->
            when (event) {
                AddOtpEvent.UriParsed -> {
                    Toast.makeText(context, uriParsedMessage, Toast.LENGTH_SHORT).show()
                }

                AddOtpEvent.UriParseFailed -> {
                    snackbarHostState.showSnackbar(uriParseFailedMessage)
                }
            }
        }
    }

    AddOtpEditorScreen(
        state = uiState,
        onAction = viewModel::onAction,
        onBack = onBack,
        onScan = { overlay = OtpEditorOverlay.Scanner },
        snackbarHostState = snackbarHostState,
        saveActionModifier = saveActionModifier,
        isEditingExisting = editSeedToken != null,
    )

    AnimatedVisibility(
        visible = overlay == OtpEditorOverlay.Scanner,
        enter =
            fadeIn(animationSpec = MaterialTheme.motionScheme.fastEffectsSpec()) +
                slideInVertically(
                    animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec(),
                    initialOffsetY = { it },
                ),
        exit =
            fadeOut(animationSpec = MaterialTheme.motionScheme.fastEffectsSpec()) +
                slideOutVertically(
                    animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec(),
                    targetOffsetY = { it },
                ),
    ) {
        ScannerRoute(
            resolveResult = { rawValue ->
                val config = (OtpAuthUriCodec.parse(rawValue) as? OtpUriParseResult.Parsed)?.config
                val presentation =
                    if (config != null) {
                        ScannerResultPresentation(
                            showCopyAction = false,
                            primaryActionLabel = ScannerPrimaryActionLabel.Use,
                        )
                    } else {
                        ScannerResultPresentation.DEFAULT
                    }
                ScannerResultPlan(
                    presentation = presentation,
                    confirm = {
                        if (config != null && viewModel.tryApplyScannedConfig(config)) {
                            ScannerResultHandling.Handled
                        } else {
                            ScannerResultHandling.Rejected
                        }
                    },
                )
            },
            onDismiss = { overlay = OtpEditorOverlay.None },
        )
    }
}

private sealed interface OtpEditorOverlay {
    data object None : OtpEditorOverlay

    data object Scanner : OtpEditorOverlay
}
