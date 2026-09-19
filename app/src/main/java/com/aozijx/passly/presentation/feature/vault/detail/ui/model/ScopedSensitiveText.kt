package com.aozijx.passly.presentation.feature.vault.detail.ui.model

interface ScopedSensitiveText {
    val isEmpty: Boolean
    fun <R> useChars(block: (CharArray) -> R): R

    data object Empty : ScopedSensitiveText {
        override val isEmpty = true
        override fun <R> useChars(block: (CharArray) -> R): R = block(CharArray(0))
        override fun toString() = "***"
    }
}
