package com.aozijx.passly.presentation.ui.shared.components.topbar

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.aozijx.passly.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun passlyTopAppBarColors(): TopAppBarColors {
    val colors = MaterialTheme.colorScheme
    return TopAppBarDefaults.topAppBarColors(
        containerColor = colors.surface,
        scrolledContainerColor = colors.surfaceContainer,
        navigationIconContentColor = colors.onSurface,
        titleContentColor = colors.onSurface,
        actionIconContentColor = colors.onSurfaceVariant,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PasslyNavigationTopBar(
    title: String,
    onNavigateBack: (() -> Unit)?,
    navigationEnabled: Boolean = true,
) {
    TopAppBar(
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
        },
        navigationIcon = {
            onNavigateBack?.let { navigateBack ->
                IconButton(onClick = navigateBack, enabled = navigationEnabled) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.back),
                    )
                }
            }
        },
        colors = passlyTopAppBarColors(),
    )
}
