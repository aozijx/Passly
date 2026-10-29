package com.aozijx.passly.feature.autofill.legacy

import com.aozijx.passly.domain.autofill.model.AutofillInboxEnqueueOutcome
import com.aozijx.passly.domain.autofill.model.AutofillInboxSource
import com.aozijx.passly.domain.autofill.model.AutofillRequest
import com.aozijx.passly.domain.autofill.model.AutofillSaveDraft
import com.aozijx.passly.domain.autofill.model.AutofillSaveOutcome
import com.aozijx.passly.domain.autofill.model.AutofillSource
import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.settings.model.AutofillSettings
import com.aozijx.passly.domain.settings.model.InteractionSettings
import com.aozijx.passly.domain.settings.port.InteractionSettingsSource
import com.aozijx.passly.feature.autofill.core.AutofillSaveDraftExtraction
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class LegacySaveFlowTest {
    @Test
    fun unlockedSessionPersistsDirectly() = runBlocking {
        val fixture = fixture(canPersistImmediately = true)

        assertEquals(LegacySavePreparation.Saved, fixture.flow.prepare(request()))
        assertEquals(1, fixture.saveCalls)
        assertEquals(0, fixture.queueCalls)
    }

    @Test
    fun lockedSessionQueuesWithoutDatabaseWrite() = runBlocking {
        val fixture = fixture(inboxOutcome = AutofillInboxEnqueueOutcome.Queued)

        assertEquals(LegacySavePreparation.Queued, fixture.flow.prepare(request()))
        assertEquals(1, fixture.queueCalls)
        assertEquals(0, fixture.saveCalls)
    }

    @Test
    fun lockedSessionWithoutProvisionedInboxKeyRejectsSave() = runBlocking {
        val fixture = fixture()

        assertEquals(LegacySavePreparation.Failed, fixture.flow.prepare(request()))
        assertEquals(1, fixture.queueCalls)
        assertEquals(0, fixture.saveCalls)
    }

    @Test
    fun fullQueueRejectsNewSaveWithoutEvictingOrWriting() = runBlocking {
        val fixture = fixture(inboxOutcome = AutofillInboxEnqueueOutcome.Full)

        assertEquals(LegacySavePreparation.Failed, fixture.flow.prepare(request()))
        assertEquals(1, fixture.queueCalls)
        assertEquals(0, fixture.saveCalls)
    }

    @Test
    fun disabledSavePolicyDoesNotQueueOrPersist() = runBlocking {
        val fixture = fixture()
        fixture.settings.value = settings(saveEnabled = false)

        assertEquals(LegacySavePreparation.Disabled, fixture.flow.prepare(request()))
        assertEquals(0, fixture.queueCalls)
        assertEquals(0, fixture.saveCalls)
    }

    @Test
    fun invalidDraftDoesNotQueueOrPersist() = runBlocking {
        val fixture = fixture(
            extraction = AutofillSaveDraftExtraction.Rejected(
                com.aozijx.passly.feature.autofill.core.AutofillSaveDraftRejection.PASSWORD_MISSING,
            ),
        )

        assertEquals(LegacySavePreparation.InvalidDraft, fixture.flow.prepare(request()))
        assertEquals(0, fixture.queueCalls)
        assertEquals(0, fixture.saveCalls)
    }

    @Test
    fun persistenceFailureIsReported() = runBlocking {
        val fixture = fixture(
            canPersistImmediately = true,
            saveOutcome = AutofillSaveOutcome.Failed(
                com.aozijx.passly.domain.autofill.model.AutofillOperationFailure.PERSISTENCE_FAILED,
            ),
        )

        assertEquals(LegacySavePreparation.Failed, fixture.flow.prepare(request()))
        assertEquals(1, fixture.saveCalls)
    }

    @Test
    fun directPersistenceExceptionReachesServiceBoundaryUnchanged() = runBlocking {
        val failure = IllegalStateException("password=must-not-be-logged")
        val fixture = fixture(canPersistImmediately = true, saveFailure = failure)

        val thrown = try {
            fixture.flow.prepare(request())
            null
        } catch (caught: IllegalStateException) {
            caught
        }

        assertSame(failure, thrown)
    }

    private fun fixture(
        canPersistImmediately: Boolean = false,
        inboxOutcome: AutofillInboxEnqueueOutcome = AutofillInboxEnqueueOutcome.NotProvisioned,
        saveOutcome: AutofillSaveOutcome = AutofillSaveOutcome.Saved(EntryId("saved")),
        extraction: AutofillSaveDraftExtraction = AutofillSaveDraftExtraction.Ready(draft()),
        saveFailure: RuntimeException? = null,
    ): Fixture {
        val settings = MutableStateFlow(settings())
        var queueCalls = 0
        var saveCalls = 0
        val flow = LegacySaveFlow(
            settingsSource = object : InteractionSettingsSource {
                override val interaction = settings
            },
            canPersistImmediately = { canPersistImmediately },
            enqueueInbox = { source, _ ->
                assertEquals(AutofillInboxSource.LEGACY_SERVICE, source)
                queueCalls++
                inboxOutcome
            },
            extractDraft = { extraction },
            saveCredential = {
                saveCalls++
                saveFailure?.let { throw it }
                saveOutcome
            },
        )
        return Fixture(flow, settings, { queueCalls }, { saveCalls })
    }

    private fun settings(saveEnabled: Boolean = true) = InteractionSettings(
        autofill = AutofillSettings(enabled = true, savePromptsEnabled = saveEnabled),
    )

    private fun request() = AutofillRequest(
        packageName = "com.example",
        domain = "example.com",
        fields = emptyList(),
        source = AutofillSource.AUTOFILL_SERVICE,
    )

    private fun draft() = AutofillSaveDraft(
        packageName = "com.example",
        webDomain = "example.com",
        pageTitle = null,
        username = "user",
        password = "password",
    )

    private data class Fixture(
        val flow: LegacySaveFlow,
        val settings: MutableStateFlow<InteractionSettings>,
        private val queueCallsProvider: () -> Int,
        private val saveCallsProvider: () -> Int,
    ) {
        val queueCalls: Int get() = queueCallsProvider()
        val saveCalls: Int get() = saveCallsProvider()
    }
}
