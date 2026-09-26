package com.aozijx.passly.presentation.feature.vault.detail.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import com.aozijx.passly.presentation.feature.vault.detail.DetailUiAction
import com.aozijx.passly.presentation.feature.vault.detail.ui.component.DetailTopBar
import com.aozijx.passly.presentation.feature.vault.detail.ui.model.DetailPresentationModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    model: DetailPresentationModel,
    otpQrUri: String?,
    onBack: () -> Unit,
    onAction: (DetailUiAction) -> Unit,
    onOpenRelatedEntry: (String) -> Unit,
    onOtpQrDismissed: () -> Unit,
    onFaviconUploadRequested: () -> Unit,
) {
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            DetailTopBar(
                model = model.header,
                scrollBehavior = scrollBehavior,
                onTitleChanged = { onAction(DetailUiAction.UpdateEditedTitle(it)) },
                onTitleEditStarted = { onAction(DetailUiAction.StartTitleEdit) },
                onTitleSaved = { onAction(DetailUiAction.SaveTitle) },
                onFavoriteToggled = { onAction(DetailUiAction.ToggleFavorite) },
                onBack = onBack,
            )
        },
    ) { innerPadding ->
        DetailBody(
            model = model.content,
            otpQrUri = otpQrUri,
            onAction = onAction,
            onOpenRelatedEntry = onOpenRelatedEntry,
            onOtpQrDismissed = onOtpQrDismissed,
            modifier = Modifier.padding(innerPadding),
        )
    }

    DetailEditorOverlays(
        model = model.overlays,
        onAction = onAction,
        onFaviconUploadRequested = onFaviconUploadRequested,
    )
}
