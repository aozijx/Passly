package com.aozijx.passly.feature.vault.presentation.detail.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.aozijx.passly.feature.vault.presentation.detail.model.DetailRevisionFieldLabelUiState

@Composable
internal fun DetailRevisionFieldLabelUiState.resolveText(): String =
    when (this) {
        is DetailRevisionFieldLabelUiState.Resource -> stringResource(resource)
        is DetailRevisionFieldLabelUiState.Named -> stringResource(resource, name)
    }
