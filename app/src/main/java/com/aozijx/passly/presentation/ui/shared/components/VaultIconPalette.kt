package com.aozijx.passly.presentation.ui.shared.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

internal enum class VaultIconColorToken(val storageValue: String) {
    PRIMARY("primary"),
    SECONDARY("secondary"),
    TERTIARY("tertiary"),
    ERROR("error"),
    NEUTRAL("neutral");

    companion object {
        fun fromStorage(value: String?): VaultIconColorToken? = entries.firstOrNull {
            it.storageValue == value
        }
    }
}

@Composable
internal fun iconColorForStorageToken(value: String?, automatic: Color): Color =
    when (VaultIconColorToken.fromStorage(value)) {
        VaultIconColorToken.PRIMARY -> MaterialTheme.colorScheme.primary
        VaultIconColorToken.SECONDARY -> MaterialTheme.colorScheme.secondary
        VaultIconColorToken.TERTIARY -> MaterialTheme.colorScheme.tertiary
        VaultIconColorToken.ERROR -> MaterialTheme.colorScheme.error
        VaultIconColorToken.NEUTRAL -> MaterialTheme.colorScheme.onSurfaceVariant
        null -> automatic
    }
