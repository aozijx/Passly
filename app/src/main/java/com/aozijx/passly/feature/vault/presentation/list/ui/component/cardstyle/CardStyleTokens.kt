package com.aozijx.passly.feature.vault.presentation.list.ui.component.cardstyle

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object CardStyleTokens {
    object Base {
        const val CONTAINER_ALPHA = 0.3f
        val contentPadding = 16.dp
        val iconTextSpacing = 20.dp
    }

    object Password {
        val elevation = 3.dp
        val contentPadding = 16.dp
        val iconTextSpacing = 20.dp
        val maxWidth = 480.dp

        const val IMAGE_OVERLAY_ALPHA = 0.22f

        const val NO_IMAGE_TOP_OVERLAY_ALPHA = 0.20f
        const val WITH_IMAGE_TOP_OVERLAY_ALPHA = 0.30f
        const val NO_IMAGE_BOTTOM_OVERLAY_ALPHA = 0.88f
        const val WITH_IMAGE_BOTTOM_OVERLAY_ALPHA = 0.78f

        const val TERTIARY_TEXT_ALPHA = 0.78f

    }

    object Totp {
        val elevation = 1.dp
        val contentPadding = 16.dp

        val rowSpacing = 16.dp
        val iconContainerCorner = 16.dp
        val iconContainerSize = 52.dp

        val codeColumnSpacing = 4.dp
        val codeFontSize = 20.sp
        val codeLetterSpacing = 1.sp

        val progressRowSpacing = 6.dp
        val progressSize = 12.dp
        val progressStrokeWidth = 2.dp
        val lockIconSize = 28.dp

        const val SURFACE_GRADIENT_TOP_ALPHA = 0.35f
        const val ICON_CONTAINER_ALPHA = 0.5f
        const val PROGRESS_TRACK_ALPHA = 0.5f
        const val LOCK_ICON_TINT_ALPHA = 0.4f
    }
}
