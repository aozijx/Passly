package com.aozijx.passly.feature.vault.entry

import com.aozijx.passly.core.error.model.SessionModeRestricted
import com.aozijx.passly.core.error.model.ValidationError
import com.aozijx.passly.core.error.result.AppResult
import com.aozijx.passly.domain.access.port.SecureSessionAccessState
import com.aozijx.passly.domain.autofill.port.AutofillEntryWriter
import com.aozijx.passly.domain.entry.model.Entry
import com.aozijx.passly.domain.entry.model.EntryDraft
import com.aozijx.passly.domain.entry.model.EntryDraftTarget
import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.entry.model.EntryIdentity
import com.aozijx.passly.domain.entry.model.EntryTimestamps
import com.aozijx.passly.domain.entry.model.EntryVersion
import com.aozijx.passly.domain.entry.policy.EntryTypeDefinitions
import com.aozijx.passly.domain.entry.policy.EntryValidation
import com.aozijx.passly.domain.entry.port.EntryCommandRepository
import com.github.f4b6a3.uuid.UuidCreator
import javax.inject.Inject

class CreateEntryUseCase internal constructor(
    private val entryCommandRepository: EntryCommandRepository,
    private val secureSessionAccessState: SecureSessionAccessState,
    private val materializer: EntryDraftMaterializer,
    private val autofillEntryWriter: AutofillEntryWriter = UnavailableAutofillEntryWriter,
    private val idFactory: () -> EntryId,
    private val clock: () -> Long,
) {
    @Inject
    constructor(
        entryCommandRepository: EntryCommandRepository,
        secureSessionAccessState: SecureSessionAccessState,
        materializer: EntryDraftMaterializer,
        autofillEntryWriter: AutofillEntryWriter,
    ) : this(
        entryCommandRepository = entryCommandRepository,
        secureSessionAccessState = secureSessionAccessState,
        materializer = materializer,
        autofillEntryWriter = autofillEntryWriter,
        idFactory = { EntryId(UuidCreator.getTimeOrderedEpoch().toString()) },
        clock = System::currentTimeMillis,
    )

    internal constructor(
        entryCommandRepository: EntryCommandRepository,
        secureSessionAccessState: SecureSessionAccessState,
        materializer: EntryDraftMaterializer,
    ) : this(
        entryCommandRepository = entryCommandRepository,
        secureSessionAccessState = secureSessionAccessState,
        materializer = materializer,
        autofillEntryWriter = UnavailableAutofillEntryWriter,
        idFactory = { EntryId(UuidCreator.getTimeOrderedEpoch().toString()) },
        clock = System::currentTimeMillis,
    )

    suspend operator fun invoke(draft: EntryDraft): AppResult<EntryId> {
        if (!secureSessionAccessState.hasFullSecureSessionAccess()) {
            return AppResult.Failure(SessionModeRestricted())
        }
        return create(draft, entryCommandRepository::createEntry)
    }

    internal suspend fun createFromAutofill(draft: EntryDraft): AppResult<EntryId> {
        if (!secureSessionAccessState.hasAutofillSessionAccess()) {
            return AppResult.Failure(SessionModeRestricted())
        }
        return create(draft, autofillEntryWriter::create)
    }

    private suspend fun create(
        draft: EntryDraft,
        persist: suspend (Entry) -> AppResult<EntryId>,
    ): AppResult<EntryId> {
        if (draft.target !is EntryDraftTarget.New) {
            return AppResult.Failure(ValidationError())
        }
        val definition = EntryTypeDefinitions[draft.target.type]
        if (draft.missingRequiredFields(definition).isNotEmpty()) {
            return AppResult.Failure(ValidationError())
        }

        val now = clock()
        val identity = EntryIdentity(
            id = idFactory(),
            type = draft.target.type,
            version = EntryVersion.INITIAL,
            timestamps = EntryTimestamps(now),
        )
        val entry = materializer.materialize(draft, identity)
        if (EntryValidation.validate(entry).isNotEmpty()) {
            return AppResult.Failure(ValidationError())
        }
        return persist(entry)
    }
}

private object UnavailableAutofillEntryWriter : AutofillEntryWriter {
    override suspend fun create(
        entry: Entry,
    ): AppResult<EntryId> = AppResult.Failure(SessionModeRestricted())
}
