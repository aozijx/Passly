package com.aozijx.passly.presentation.feature.settings.ui.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aozijx.passly.core.ui.adaptive.LocalPasslyAdaptiveLayout
import com.aozijx.passly.presentation.shared.components.topbar.PasslyNavigationTopBar

@Composable
internal fun SettingsSecondaryPage(
    title: String,
    onBack: (() -> Unit)?,
    content: LazyListScope.() -> Unit
) {
    val adaptiveLayout = LocalPasslyAdaptiveLayout.current

    Scaffold(
        topBar = {
            PasslyNavigationTopBar(
                title = title,
                onNavigateBack = onBack,
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxHeight()
                    .widthIn(max = if (adaptiveLayout.isExpanded) 840.dp else 720.dp)
                    .fillMaxWidth()
                    .padding(horizontal = if (adaptiveLayout.isAtLeastMedium) 32.dp else 16.dp),
                content = {
                    item { Spacer(modifier = Modifier.height(8.dp)) }
                    content()
                    item { Spacer(modifier = Modifier.height(32.dp)) }
                }
            )
        }
    }
}
