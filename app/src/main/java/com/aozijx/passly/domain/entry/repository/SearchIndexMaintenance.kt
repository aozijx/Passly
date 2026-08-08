package com.aozijx.passly.domain.entry.repository

import com.aozijx.passly.core.error.result.AppResult

interface SearchIndexMaintenance {
    suspend fun rebuildIndex(force: Boolean = false): AppResult<Int>
}
