package com.aozijx.passly.presentation.ui.vault.detail

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import com.aozijx.passly.presentation.ui.vault.detail.component.DetailTopBar
import com.aozijx.passly.presentation.ui.vault.detail.model.DetailScreenUiModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    model: DetailScreenUiModel,
    onBack: () -> Unit,
    onInteraction: () -> Unit,
    onTitleChanged: (String) -> Unit,
    onTitleEditStarted: () -> Unit,
    onTitleSaved: () -> Unit,
    onFavoriteToggled: () -> Unit,
    content: @Composable (Modifier) -> Unit,
) {
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            DetailTopBar(model, scrollBehavior, onTitleChanged, onTitleEditStarted,
                onTitleSaved, onFavoriteToggled, onBack, onInteraction)
        },
    ) { innerPadding -> content(Modifier.padding(innerPadding)) }
}
