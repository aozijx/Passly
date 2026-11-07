package com.aozijx.passly.app.entry.creation

import com.aozijx.passly.core.error.result.AppResult
import com.aozijx.passly.domain.autofill.port.AutofillEntryWriter
import com.aozijx.passly.domain.entry.model.Entry
import com.aozijx.passly.domain.entry.model.EntryId

internal object NoOpAutofillEntryWriter : AutofillEntryWriter {
    override suspend fun create(entry: Entry): AppResult<EntryId> = AppResult.Success(entry.id)

    override suspend fun createFromInbox(entry: Entry): AppResult<EntryId> = AppResult.Success(entry.id)
}
