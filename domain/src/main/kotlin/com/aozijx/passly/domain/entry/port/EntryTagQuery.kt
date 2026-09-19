package com.aozijx.passly.domain.entry.port

interface EntryTagQuery {
    suspend fun findAllTags(): Set<String>
}
