package com.aozijx.passly.domain.clipboard.port

/** Clears clipboard content only when it is still owned by this application. */
fun interface OwnedClipboardCleaner {
    fun clearOwned(): Boolean
}
