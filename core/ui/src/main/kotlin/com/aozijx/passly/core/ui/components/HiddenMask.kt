package com.aozijx.passly.core.ui.components

/**
 * Fixed-length masks for read-only sensitive values.
 *
 * A fixed mask deliberately does not mirror the secret length.
 * Editable fields should continue to use PasswordVisualTransformation.
 */
object HiddenMask {
    const val DEFAULT = "••••••"
    const val SHORT = "•••"
}
