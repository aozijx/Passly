package com.aozijx.passly.presentation.ui.vault.detail.component

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.aozijx.passly.core.media.toLocalIconImageModel

@Composable
fun DetailHeader(
    iconCustomPath: String?,
    updatedAt: Long,
    onIconClick: () -> Unit,
    onTitleLongClick: () -> Unit,
) {
    val context = LocalContext.current
    val imageModel = remember(iconCustomPath, updatedAt) {
        toLocalIconImageModel(iconCustomPath)
    }
    val hasImage = !imageModel.isNullOrBlank()

    if (hasImage) {
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
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(imageModel)
                        .memoryCacheKey("${imageModel}_$updatedAt")
                        .diskCacheKey("${imageModel}_$updatedAt")
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        }
    }
}
