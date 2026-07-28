package com.aozijx.passly.core.ui.components.common

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

@Composable
fun ActionButton(
    modifier: Modifier = Modifier,
    progress: Boolean = false,
    result: Boolean? = null,
    icon: ImageVector? = null,
    containerColor: Color? = null,
    text: String = "Unknown",
    resultText: String = "Success",
    enabled: Boolean = true,
    content: @Composable (() -> Unit)? = null,
    onClick: () -> Unit = {}
) {
    FilledTonalButton(
        modifier = modifier
            .fillMaxWidth()
            .height(46.dp),
        onClick = onClick,
        enabled = enabled && !progress && result == null,
        shape = RoundedCornerShape(16.dp),
        colors = if (containerColor != null) {
            ButtonDefaults.filledTonalButtonColors(containerColor = containerColor)
        } else {
            ButtonDefaults.filledTonalButtonColors()
        }
    ) {
        AnimatedContent(
            targetState = progress to result,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "ActionButtonContent"
        ) { (loading, actionResult) ->
            when {
                loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                content != null && actionResult == null -> {
                    content()
                }

                else -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (icon != null) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.size(8.dp))
                        }
                        Text(
                            text = if (actionResult != null) resultText else text,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
            }
        }
    }
}
