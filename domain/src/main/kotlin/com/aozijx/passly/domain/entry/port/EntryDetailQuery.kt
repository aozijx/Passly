package com.aozijx.passly.domain.entry.port

import com.aozijx.passly.domain.entry.model.Entry
import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.entry.model.query.EntryDetailData
import com.aozijx.passly.domain.entry.model.query.EntryDetailSnapshot
import com.aozijx.passly.domain.entry.model.query.EntryRelationsSnapshot

interface EntryDetailQuery {
    suspend fun open(entryId: EntryId): EntryDetailSnapshot?

    suspend fun snapshot(entry: Entry): EntryDetailData

    suspend fun reloadRelations(entryId: EntryId): EntryRelationsSnapshot?
}
