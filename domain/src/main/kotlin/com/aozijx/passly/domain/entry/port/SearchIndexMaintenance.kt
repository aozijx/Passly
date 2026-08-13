package com.aozijx.passly.domain.entry.port

import com.aozijx.passly.core.error.result.AppResult

interface SearchIndexMaintenance {
    suspend fun rebuildIndex(force: Boolean = false): AppResult<Int>
}
