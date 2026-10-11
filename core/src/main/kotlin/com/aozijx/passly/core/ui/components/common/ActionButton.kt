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
    phase: ActionButtonPhase = ActionButtonPhase.Idle,
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
        enabled = enabled && phase == ActionButtonPhase.Idle,
        shape = MaterialTheme.shapes.extraLarge,
        colors = if (containerColor != null) {
            ButtonDefaults.filledTonalButtonColors(containerColor = containerColor)
        } else {
            ButtonDefaults.filledTonalButtonColors()
        }
    ) {
        AnimatedContent(
            targetState = ActionButtonVisualState(
                phase = phase,
                icon = icon,
                text = if (phase == ActionButtonPhase.Succeeded ||
                    phase == ActionButtonPhase.Failed
                ) {
                    resultText
                } else {
                    text
                },
            ),
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "ActionButtonContent"
        ) { state ->
            when (state.phase) {
                ActionButtonPhase.InProgress -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                ActionButtonPhase.Idle -> if (content != null) {
                    content()
                } else {
                    ActionButtonLabel(state.icon, state.text)
                }

                ActionButtonPhase.Succeeded,
                ActionButtonPhase.Failed -> ActionButtonLabel(state.icon, state.text)
            }
        }
    }
}

@Composable
private fun ActionButtonLabel(icon: ImageVector?, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        icon?.let {
            Icon(
                imageVector = it,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.size(8.dp))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
        )
    }
}

private data class ActionButtonVisualState(
    val phase: ActionButtonPhase,
    val icon: ImageVector?,
    val text: String
)

enum class ActionButtonPhase {
    Idle,
    InProgress,
    Succeeded,
    Failed,
}
