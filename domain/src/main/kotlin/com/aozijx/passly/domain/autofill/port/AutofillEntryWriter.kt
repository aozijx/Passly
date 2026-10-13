package com.aozijx.passly.domain.autofill.port

import com.aozijx.passly.core.error.result.AppResult
import com.aozijx.passly.domain.entry.model.Entry
import com.aozijx.passly.domain.entry.model.EntryId

/** Narrow persistence capability available only to an authorized Autofill session. */
fun interface AutofillEntryWriter {
    suspend fun create(entry: Entry): AppResult<EntryId>
}
