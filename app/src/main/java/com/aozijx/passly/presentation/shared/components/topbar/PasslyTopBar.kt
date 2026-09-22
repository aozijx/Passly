package com.aozijx.passly.presentation.shared.components.topbar

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.foundation.background
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
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
fun passlyCompactTopAppBarColors(): TopAppBarColors {
    val colors = MaterialTheme.colorScheme
    return TopAppBarDefaults.topAppBarColors(
        containerColor = Color.Transparent,
        scrolledContainerColor = Color.Transparent,
        navigationIconContentColor = colors.onSurface,
        titleContentColor = colors.onSurface,
        actionIconContentColor = colors.onSurfaceVariant,
    )
}

internal fun topAppBarContainerColor(
    containerColor: Color,
    scrolledContainerColor: Color,
    scrollProgress: Float,
): Color = lerp(
    start = containerColor,
    stop = scrolledContainerColor,
    fraction = scrollProgress.coerceIn(0f, 1f),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PasslyNavigationTopBar(
    title: String,
    onNavigateBack: (() -> Unit)?,
    navigationEnabled: Boolean = true,
) {
    TopAppBar(
        modifier = Modifier.background(MaterialTheme.colorScheme.surface),
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
        colors = passlyCompactTopAppBarColors(),
    )
}
