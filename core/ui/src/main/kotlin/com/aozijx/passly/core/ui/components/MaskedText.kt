package com.aozijx.passly.core.ui.components

import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp

enum class MaskStyle {
    DEFAULT, SHORT
}

/**
 * A specialized text component for sensitive values.
 * Handles automatic masking when [isRevealed] is false.
 */
@Composable
fun MaskedText(
    text: String?,
    isRevealed: Boolean,
    modifier: Modifier = Modifier,
    maskStyle: MaskStyle = MaskStyle.DEFAULT,
    style: TextStyle = MaterialTheme.typography.bodyLarge,
    color: Color = LocalContentColor.current,
    textAlign: TextAlign = TextAlign.End,
    fontWeight: FontWeight = FontWeight.Bold
) {
    val mask = when (maskStyle) {
        MaskStyle.DEFAULT -> "••••••"
        MaskStyle.SHORT -> "•••"
    }

    Text(
        text = if (isRevealed) text.orEmpty() else mask,
        modifier = modifier,
        style = style,
        color = color,
        textAlign = textAlign,
        fontWeight = fontWeight,
        letterSpacing = if (isRevealed) 0.sp else 3.sp,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}
