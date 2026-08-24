package com.aozijx.passly.presentation.ui.vault.detail.model

interface ScopedSensitiveText {
    val isEmpty: Boolean
    fun <R> useChars(block: (CharArray) -> R): R

    data object Empty : ScopedSensitiveText {
        override val isEmpty = true
        override fun <R> useChars(block: (CharArray) -> R): R = block(CharArray(0))
        override fun toString() = "***"
    }
}
