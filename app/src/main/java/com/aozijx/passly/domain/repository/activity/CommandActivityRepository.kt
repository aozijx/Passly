package com.aozijx.passly.domain.repository.activity

interface CommandActivityRepository {
    suspend fun deleteByEntryId(entryId: String)
    suspend fun deleteBefore(timestamp: Long)
}
