package com.aozijx.passly.feature.settings.presentation.security

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** Owns one mutable password buffer and clears its previous contents on replacement. */
internal class SensitivePasswordDraft {
    private var value by mutableStateOf(charArrayOf())

    val size: Int
        get() = value.size

    val isNotEmpty: Boolean
        get() = value.isNotEmpty()

    fun replace(ownedValue: CharArray) {
        if (value === ownedValue) return
        clear()
        value = ownedValue
    }

    fun snapshot(): String = String(value)

    fun copy(): CharArray = value.copyOf()

    fun contentEquals(other: SensitivePasswordDraft): Boolean = value.contentEquals(other.value)

    fun clear() {
        value.fill('\u0000')
        value = charArrayOf()
    }
}
