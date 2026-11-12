package com.aozijx.passly.presentation.shared.components

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
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.aozijx.passly.core.platform.packageinfo.rememberInstalledAppIconBitmap
import com.aozijx.passly.presentation.shared.entryvisual.VaultItemAssociatedAppUiState
import com.aozijx.passly.presentation.shared.entryvisual.VaultItemIconGlyphUiState
import com.aozijx.passly.presentation.shared.entryvisual.VaultItemIconImageUiState
import com.aozijx.passly.presentation.shared.entryvisual.VaultItemIconTintUiState
import com.aozijx.passly.presentation.shared.entryvisual.VaultItemIconUiModel

@Composable
internal fun VaultItemIcon(
    modifier: Modifier = Modifier,
    model: VaultItemIconUiModel,
) {
    val tint = MaterialTheme.colorScheme.onSecondaryContainer
    val appIconBitmap =
        when (val associatedApp = model.associatedApp) {
            VaultItemAssociatedAppUiState.None -> null
            is VaultItemAssociatedAppUiState.Package -> rememberInstalledAppIconBitmap(associatedApp.name)
        }
    val appIconPainter =
        remember(appIconBitmap) {
            appIconBitmap?.let(::BitmapPainter)
        }
    val fallbackIconVector =
        when (val glyph = model.glyph) {
            is VaultItemIconGlyphUiState.Named -> glyph.imageVector
            VaultItemIconGlyphUiState.CategoryFallback -> VaultIcons.getIconByCategory(model.visualCategory)
        }
    val fallbackPainter = rememberVectorPainter(fallbackIconVector)
    val placeholderPainter = appIconPainter ?: fallbackPainter
    val glyphTint =
        when (val icon = model.glyph) {
            VaultItemIconGlyphUiState.CategoryFallback -> {
                tint
            }

            is VaultItemIconGlyphUiState.Named -> {
                when (val iconTint = icon.tint) {
                    VaultItemIconTintUiState.ThemeDefault -> tint
                    is VaultItemIconTintUiState.StorageToken -> iconColorForStorageToken(iconTint.value, tint)
                }
            }
        }
    Box(
        modifier = modifier.size(36.dp),
        contentAlignment = Alignment.Center,
    ) {
        when (val image = model.image) {
            is VaultItemIconImageUiState.Custom -> {
                AsyncImage(
                    model = image.model.uri,
                    contentDescription = null,
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .clip(CircleShape),
                    contentScale = ContentScale.Fit,
                    placeholder = placeholderPainter,
                    error = placeholderPainter,
                )
            }

            VaultItemIconImageUiState.Default -> {
                when (val appPainter = appIconPainter) {
                    null -> {
                        Icon(
                            imageVector = fallbackIconVector,
                            contentDescription = null,
                            tint = glyphTint,
                            modifier = Modifier.size(24.dp),
                        )
                    }

                    else -> {
                        Image(
                            painter = appPainter,
                            contentDescription = null,
                            modifier =
                                Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape),
                            contentScale = ContentScale.Fit,
                        )
                    }
                }
            }
        }
    }
}
