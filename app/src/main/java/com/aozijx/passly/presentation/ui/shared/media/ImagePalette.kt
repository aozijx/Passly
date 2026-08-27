package com.aozijx.passly.presentation.ui.shared.media

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.graphics.drawable.toBitmap
import androidx.palette.graphics.Palette
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult

data class ImagePaletteColors(
    val accent: Color,
    val onAccent: Color
)

private object ImagePaletteCache {
    private const val MAX_SIZE = 64
    private val values = object : LinkedHashMap<String, ImagePaletteColors>(
        MAX_SIZE,
        0.75f,
        true
    ) {
        override fun removeEldestEntry(
            eldest: MutableMap.MutableEntry<String, ImagePaletteColors>
        ): Boolean = size > MAX_SIZE
    }

    @Synchronized
    fun get(key: String): ImagePaletteColors? = values[key]

    @Synchronized
    fun put(key: String, colors: ImagePaletteColors) {
        values[key] = colors
    }
}

@Composable
fun rememberImagePaletteColors(
    imageModel: Any?,
    cacheKey: Any? = imageModel
): ImagePaletteColors? {
    val context = LocalContext.current
    val resolvedKey = cacheKey?.toString().orEmpty()
    var colors by remember(resolvedKey) {
        mutableStateOf(ImagePaletteCache.get(resolvedKey))
    }

    LaunchedEffect(imageModel, resolvedKey) {
        colors = ImagePaletteCache.get(resolvedKey)
        if (imageModel == null || resolvedKey.isBlank() || colors != null) return@LaunchedEffect

        val bitmap = runCatching {
            val request = ImageRequest.Builder(context)
                .data(imageModel)
                .allowHardware(false)
                .build()
            val result = context.imageLoader.execute(request)
            (result as? SuccessResult)?.drawable?.toBitmap()
        }.getOrNull() ?: return@LaunchedEffect

        val swatch = Palette.from(bitmap)
            .clearFilters()
            .generate()
            .let { palette ->
                palette.vibrantSwatch
                    ?: palette.dominantSwatch
                    ?: palette.mutedSwatch
                    ?: palette.darkVibrantSwatch
                    ?: palette.darkMutedSwatch
            } ?: return@LaunchedEffect

        colors = ImagePaletteColors(
            accent = Color(swatch.rgb),
            onAccent = Color(swatch.bodyTextColor)
        ).also {
            ImagePaletteCache.put(resolvedKey, it)
        }
    }

    return colors
}
