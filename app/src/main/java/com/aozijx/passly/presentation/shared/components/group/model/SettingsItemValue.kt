package com.aozijx.passly.presentation.shared.components.group.model

/** Explicit trailing-value state for a navigation settings row. */
sealed interface SettingsItemValue {
    data object Empty : SettingsItemValue

    data object Loading : SettingsItemValue

    data object Unavailable : SettingsItemValue

    data class Text(
        val value: String,
    ) : SettingsItemValue {
        init {
            require(value.isNotBlank()) { "Text value must not be blank" }
        }
    }
}
