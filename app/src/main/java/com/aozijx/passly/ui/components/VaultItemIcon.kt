package com.aozijx.passly.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.aozijx.passly.core.media.FaviconUtils
import com.aozijx.passly.core.media.ImageResolver.toLocalIconImageModel
import com.aozijx.passly.core.platform.rememberAppIcon
import com.aozijx.passly.domain.model.VaultIconable

@Composable
fun VaultItemIcon(
    modifier: Modifier = Modifier,
    iconable: VaultIconable,
    tint: Color = MaterialTheme.colorScheme.onSecondaryContainer
) {
    val context = LocalContext.current

    val appIconPainter = rememberAppIcon(iconable.associatedAppPackage)

    val fallbackIconVector = remember(iconable.iconName, iconable.category) {
        getCategoryIcon(context, iconable.category)
    }
    val fallbackPainter = rememberVectorPainter(fallbackIconVector)

    val placeholderPainter = appIconPainter ?: fallbackPainter

    val customModel = remember(iconable.iconCustomPath) { toLocalIconImageModel(iconable.iconCustomPath) }
    val domainUrl = remember(iconable.associatedDomain) {
        iconable.associatedDomain?.let { "https://${FaviconUtils.cleanDomain(it)}/favicon.ico" }
    }

    Box(
        modifier = modifier.size(36.dp), contentAlignment = Alignment.Center
    ) {
        when {
            customModel != null || domainUrl != null -> {
                AsyncImage(
                    model = customModel ?: domainUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop,
                    placeholder = placeholderPainter,
                    error = placeholderPainter
                )
            }

            appIconPainter != null -> {
                Image(
                    painter = appIconPainter,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape),
                    contentScale = ContentScale.Fit
                )
            }

            else -> {
                Icon(
                    imageVector = fallbackIconVector,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}