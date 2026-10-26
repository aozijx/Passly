package com.aozijx.passly.feature.vault.entry

import com.aozijx.passly.core.error.model.SessionModeRestricted
import com.aozijx.passly.core.error.model.ValidationError
import com.aozijx.passly.core.error.result.AppResult
import com.aozijx.passly.core.observability.Logger
import com.aozijx.passly.core.observability.LogStatements
import com.aozijx.passly.core.observability.Operations
import com.aozijx.passly.core.observability.monotonicTimeMs
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
import com.aozijx.passly.domain.entry.policy.EntryDraftMaterializer
import com.aozijx.passly.domain.entry.policy.EntryValidation
import com.aozijx.passly.domain.entry.port.EntryCommandRepository
import com.github.f4b6a3.uuid.UuidCreator
import javax.inject.Inject
import kotlinx.coroutines.CancellationException

class CreateEntryUseCase internal constructor(
    private val entryCommandRepository: EntryCommandRepository,
    private val secureSessionAccessState: SecureSessionAccessState,
    private val materializer: EntryDraftMaterializer,
    private val autofillEntryWriter: AutofillEntryWriter = UnavailableAutofillEntryWriter,
    private val idFactory: () -> EntryId,
    private val clock: () -> Long,
    private val logger: Logger,
    private val monotonicClock: () -> Long,
) {
    @Inject
    constructor(
        entryCommandRepository: EntryCommandRepository,
        secureSessionAccessState: SecureSessionAccessState,
        materializer: EntryDraftMaterializer,
        autofillEntryWriter: AutofillEntryWriter,
        logger: Logger,
    ) : this(
        entryCommandRepository = entryCommandRepository,
        secureSessionAccessState = secureSessionAccessState,
        materializer = materializer,
        autofillEntryWriter = autofillEntryWriter,
        idFactory = { EntryId(UuidCreator.getTimeOrderedEpoch().toString()) },
        clock = System::currentTimeMillis,
        logger = logger,
        monotonicClock = ::monotonicTimeMs,
    )

    internal constructor(
        entryCommandRepository: EntryCommandRepository,
        secureSessionAccessState: SecureSessionAccessState,
        materializer: EntryDraftMaterializer,
        logger: Logger,
    ) : this(
        entryCommandRepository = entryCommandRepository,
        secureSessionAccessState = secureSessionAccessState,
        materializer = materializer,
        autofillEntryWriter = UnavailableAutofillEntryWriter,
        idFactory = { EntryId(UuidCreator.getTimeOrderedEpoch().toString()) },
        clock = System::currentTimeMillis,
        logger = logger,
        monotonicClock = ::monotonicTimeMs,
    )

    suspend operator fun invoke(draft: EntryDraft): AppResult<EntryId> = observeCreate(
        draft = draft,
        trigger = EntryCreateTrigger.USER,
        hasAccess = secureSessionAccessState::hasFullSecureSessionAccess,
        persist = entryCommandRepository::createEntry,
    )

    internal suspend fun createFromAutofill(draft: EntryDraft): AppResult<EntryId> = observeCreate(
        draft = draft,
        trigger = EntryCreateTrigger.AUTOFILL,
        hasAccess = secureSessionAccessState::hasAutofillSessionAccess,
        persist = autofillEntryWriter::create,
    )

    private suspend fun observeCreate(
        draft: EntryDraft,
        trigger: EntryCreateTrigger,
        hasAccess: () -> Boolean,
        persist: suspend (Entry) -> AppResult<EntryId>,
    ): AppResult<EntryId> = logger.inSpan(Operations.VaultEntryCreate) {
        val entryType = draft.target.type
        val startedAt = monotonicClock()
        logger.log(LogStatements.Vault.entryCreateStarted(entryType, trigger))
        val result = try {
            if (hasAccess()) {
                create(draft, persist)
            } else {
                AppResult.Failure(SessionModeRestricted())
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Exception) {
            logger.log(
                LogStatements.Vault.entryCreateFailed(
                    failure,
                    entryType,
                    trigger,
                    monotonicClock() - startedAt,
                ),
            )
            throw failure
        }
        val elapsedMs = monotonicClock() - startedAt
        when (result) {
            is AppResult.Success -> logger.log(
                LogStatements.Vault.entryCreateCompleted(entryType, trigger, elapsedMs),
            )
            is AppResult.Failure -> logger.log(
                LogStatements.Vault.entryCreateFailed(result.error, entryType, trigger, elapsedMs),
            )
        }
        result
    }

    private suspend fun create(
        draft: EntryDraft,
        persist: suspend (Entry) -> AppResult<EntryId>,
    ): AppResult<EntryId> {
        if (draft.target !is EntryDraftTarget.New) {
            return AppResult.Failure(ValidationError())
        }
        val definition = EntryTypeDefinitions[draft.target.type]
        if (draft.missingRequiredFields(definition).isNotEmpty() || draft.invalidProvidedFields(definition).isNotEmpty()) {
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

private enum class EntryCreateTrigger { USER, AUTOFILL }

private object UnavailableAutofillEntryWriter : AutofillEntryWriter {
    override suspend fun create(
        entry: Entry,
    ): AppResult<EntryId> = AppResult.Failure(SessionModeRestricted())
}
