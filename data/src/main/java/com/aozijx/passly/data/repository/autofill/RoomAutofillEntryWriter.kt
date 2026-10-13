package com.aozijx.passly.data.repository.autofill

import com.aozijx.passly.core.error.result.AppResult
import com.aozijx.passly.data.repository.entry.command.CreateEntryExecutor
import com.aozijx.passly.domain.autofill.port.AutofillEntryWriter
import com.aozijx.passly.domain.entry.model.Entry
import com.aozijx.passly.domain.entry.model.EntryId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class RoomAutofillEntryWriter @Inject constructor(
    private val createEntryExecutor: CreateEntryExecutor,
) : AutofillEntryWriter {
    override suspend fun create(entry: Entry): AppResult<EntryId> =
        createEntryExecutor.executeForAutofill(entry)
}
