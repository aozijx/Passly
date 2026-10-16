package com.aozijx.passly.feature.settings.data

/** Feature-facing operations for application-owned cache data. */
interface AppCacheStore {
    suspend fun sizeBytes(): Long
    suspend fun clear()
}
