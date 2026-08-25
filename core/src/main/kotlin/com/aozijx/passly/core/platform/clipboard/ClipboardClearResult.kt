package com.aozijx.passly.core.platform.clipboard

sealed interface ClipboardClearResult {
    data object Cleared : ClipboardClearResult
    data object Empty : ClipboardClearResult
    data object NotOwned : ClipboardClearResult
    data class Failed(val cause: Throwable) : ClipboardClearResult
}
