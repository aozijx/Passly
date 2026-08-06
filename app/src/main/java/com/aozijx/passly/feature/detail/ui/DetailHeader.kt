package com.aozijx.passly.feature.detail.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.aozijx.passly.core.media.rememberImagePaletteColors
import com.aozijx.passly.core.media.toLocalIconImageModel
import com.aozijx.passly.core.ui.components.VaultItemIcon
import com.aozijx.passly.domain.entry.model.VaultEntry

@Composable
fun DetailHeader(
    item: VaultEntry,
    onIconClick: () -> Unit,
    onTitleLongClick: () -> Unit,
    onMoreClick: (() -> Unit)? = null,
    trailingText: String? = null
) {
    val context = LocalContext.current
    val imageModel = remember(item.iconCustomPath, item.updatedAt) {
        toLocalIconImageModel(item.iconCustomPath)
    }
    val hasImage = !imageModel.isNullOrBlank()
    val palette = rememberImagePaletteColors(
        imageModel = imageModel,
        cacheKey = "${imageModel.orEmpty()}_${item.updatedAt}"
    )
    val accent = palette?.accent ?: MaterialTheme.colorScheme.primary
    val contentColor = palette?.onAccent ?: MaterialTheme.colorScheme.onPrimary
    val fallbackTop = MaterialTheme.colorScheme.primaryContainer
    val fallbackBottom = MaterialTheme.colorScheme.surfaceVariant

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(204.dp)
                .clip(MaterialTheme.shapes.extraLarge)
                .combinedClickable(
                    onClick = onIconClick,
                    onLongClick = onTitleLongClick
                )
        ) {
            if (hasImage) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(imageModel)
                        .memoryCacheKey("${imageModel}_${item.updatedAt}")
                        .diskCacheKey("${imageModel}_${item.updatedAt}")
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }

            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            colors = if (hasImage) {
                                listOf(
                                    accent.copy(alpha = 0.18f),
                                    Color.Black.copy(alpha = 0.68f)
                                )
                            } else {
                                listOf(fallbackTop, fallbackBottom)
                            }
                        )
                    )
            )

            onMoreClick?.let { click ->
                Surface(
                    onClick = click,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
                    tonalElevation = 4.dp
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = null,
                        modifier = Modifier
                            .padding(8.dp)
                            .size(20.dp)
                    )
                }
            }

            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = if (hasImage) {
                        Color.White.copy(alpha = 0.18f)
                    } else {
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.86f)
                    },
                    tonalElevation = 0.dp,
                    modifier = Modifier.size(56.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        VaultItemIcon(
                            modifier = Modifier.size(34.dp),
                            iconable = item,
                            tint = if (hasImage) contentColor else MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (hasImage) contentColor else MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    val subtitle = item.username.takeIf { it.isNotBlank() }
                    if (subtitle != null) {
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (hasImage) {
                                contentColor.copy(alpha = 0.82f)
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                trailingText?.let {
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = it,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (hasImage) {
                            contentColor.copy(alpha = 0.84f)
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
