package com.aozijx.passly.core.platform.clipboard

interface SecureClipboard {
    fun copySensitive(text: String, clearAfterSeconds: Int?)
    fun clearOwned(): ClipboardClearResult
}
