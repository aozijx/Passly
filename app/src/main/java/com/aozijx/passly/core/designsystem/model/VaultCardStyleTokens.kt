package com.aozijx.passly.core.designsystem.model

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Shared visual tokens for vault list cards.
 */
object VaultCardStyleTokens {

    object Base {
        val corner = 16.dp
        const val CONTAINER_ALPHA = 0.3f
        val contentPadding = 16.dp
        val iconTextSpacing = 20.dp
    }

    object Password {
        val corner = 16.dp
        val elevation = 3.dp
        val contentPadding = 16.dp
        val iconTextSpacing = 20.dp

        const val IMAGE_OVERLAY_ALPHA = 0.22f
        const val CHIP_BG_ALPHA = 0.82f
        const val CHIP_FALLBACK_BG_ALPHA = 0.92f

        const val NO_IMAGE_TOP_OVERLAY_ALPHA = 0.20f
        const val WITH_IMAGE_TOP_OVERLAY_ALPHA = 0.30f
        const val NO_IMAGE_BOTTOM_OVERLAY_ALPHA = 0.88f
        const val WITH_IMAGE_BOTTOM_OVERLAY_ALPHA = 0.78f

        const val TERTIARY_TEXT_ALPHA = 0.78f

        val chipCorner = 999.dp
        val chipHorizontalPadding = 9.dp
        val chipVerticalPadding = 4.dp
        val chipIconSize = 12.dp
        val chipIconTextSpacing = 4.dp
    }

    object Totp {
        val corner = 24.dp
        val elevation = 1.dp
        val contentPadding = 16.dp
        
        val marginHorizontal = 16.dp
        val marginVertical = 6.dp

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
