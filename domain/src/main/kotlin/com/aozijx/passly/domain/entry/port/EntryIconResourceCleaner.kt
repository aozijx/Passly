package com.aozijx.passly.domain.entry.port

/** Schedules reference-checked cleanup. Unavailable storage defers the request instead of deleting blindly. */
fun interface EntryIconResourceCleaner {
    fun discardUnreferencedIcon(path: String)
}
