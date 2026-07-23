package com.aozijx.passly.domain.repository.search

import com.aozijx.passly.core.error.AppResult

interface SearchIndexMaintenance {
    suspend fun rebuildIndex(force: Boolean = false): AppResult<Int>
}
