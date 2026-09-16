package com.aozijx.passly.feature.settings.general

/** Feature-facing operations for application-owned cache data. */
interface AppCacheStore {
    fun sizeBytes(): Long
    fun clear()
}