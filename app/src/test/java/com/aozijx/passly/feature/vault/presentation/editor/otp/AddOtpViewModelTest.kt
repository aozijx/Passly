package com.aozijx.passly.feature.vault.presentation.editor.otp

import androidx.lifecycle.ViewModelStore
import com.aozijx.passly.app.entry.creation.CreateEntryUseCase
import com.aozijx.passly.app.entry.creation.NoOpAutofillEntryWriter
import com.aozijx.passly.core.error.result.AppResult
import com.aozijx.passly.domain.access.model.AuthenticationState
import com.aozijx.passly.domain.access.port.SecureSessionAccessState
import com.aozijx.passly.domain.entry.model.Entry
import com.aozijx.passly.domain.entry.model.EntryDraftValue
import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.entry.model.EntryIdentity
import com.aozijx.passly.domain.entry.model.EntryProfile
import com.aozijx.passly.domain.entry.model.EntrySecret
import com.aozijx.passly.domain.entry.model.EntryTimestamps
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.model.EntryUpdate
import com.aozijx.passly.domain.entry.model.EntryVersion
import com.aozijx.passly.domain.entry.model.FieldKey
import com.aozijx.passly.domain.entry.model.otp.OtpConfig
import com.aozijx.passly.domain.entry.model.otp.OtpHashAlgorithm
import com.aozijx.passly.domain.entry.model.otp.OtpSecretEncoding
import com.aozijx.passly.domain.entry.model.otp.OtpType
import com.aozijx.passly.domain.entry.model.sensitive.SensitiveFieldKey
import com.aozijx.passly.domain.entry.otp.generation.OtpGenerationMaterial
import com.aozijx.passly.domain.entry.otp.generation.OtpResult
import com.aozijx.passly.domain.entry.otp.generation.toGenerationConfig
import com.aozijx.passly.domain.entry.otp.toNewEntryDraft
import com.aozijx.passly.domain.entry.policy.EntryDraftMaterializer
import com.aozijx.passly.domain.entry.port.EntryCommandRepository
import com.aozijx.passly.feature.vault.detail.EditableEntryUseCaseFixture
import com.aozijx.passly.feature.vault.otp.OtpCodeRuntime
import com.aozijx.passly.feature.vault.otp.defaultOtpRuntimePolicyReader
import com.aozijx.passly.feature.vault.presentation.editor.EditorLoadPhase
import com.aozijx.passly.feature.vault.presentation.editor.EditorSaveEffect
import com.aozijx.passly.testing.MainDispatcherRule
import com.aozijx.passly.testing.testLogger
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AddOtpViewModelTest {
    private fun createViewModel(
        create: CreateEntryUseCase = createEntryUseCase(),
        session: SecureSessionAccessState = AuthenticatedSession,
    ): AddOtpViewModel {
        val editFixture = EditableEntryUseCaseFixture.unsupportedEntry()
        return AddOtpViewModel(create, session, editFixture.editor, emptyOtpRuntime())
    }

    private fun emptyOtpRuntime() =
        OtpCodeRuntime(
            scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined),
            otpRuntimePolicyReader = defaultOtpRuntimePolicyReader(),
            codeGenerator = { _, _ -> OtpResult.Success("unused") },
            loadOtpMaterial = { null },
        )

    @Test
    fun `scanner editor and direct creation share complete identity title`() {
        val model = createViewModel()
        val config = OtpConfig(secret = "MY", issuer = " Example: West ", accountName = " alice:admin ")
        assertTrue(model.tryApplyScannedConfig(config))
        val directTitle = (config.toNewEntryDraft()[FieldKey.TITLE] as EntryDraftValue.Text).value
        assertEquals(directTitle, model.uiState.value.form.title)
        assertEquals("Example: West: alice:admin", directTitle)
    }

    @Test
    fun `scan without identity keeps the existing editor title`() {
        val model = createViewModel()
        model.onAction(AddOtpAction.TitleChanged("My title"))
        assertTrue(model.tryApplyScannedConfig(OtpConfig(secret = "MY")))
        assertEquals("My title", model.uiState.value.form.title)
    }

    @Test
    fun scannedConfigAcknowledgesOnlyAcceptedApplications() =
        runTest {
            val session = MutableSession()
            val model = createViewModel(createEntryUseCase(session = session), session)
            val config = OtpConfig(secret = "MY")
            session.state.value = AuthenticationState.Locked
            runCurrent()
            assertFalse(model.tryApplyScannedConfig(config))
            assertEquals("", model.uiState.value.form.secret)
            session.state.value = AuthenticationState.Authenticated(2L)
            runCurrent()
            assertTrue(model.tryApplyScannedConfig(config))
            assertEquals("MY", model.uiState.value.form.secret)
        }

    @Test
    fun savingEditorRejectsScannedConfigWithoutOverwritingItsDraftOrEmittingParsed() =
        runTest {
            val release = CompletableDeferred<Unit>()
            val repository =
                object : EntryCommandRepository by NoOpEntryCommandRepository {
                    override suspend fun createEntry(entry: Entry): AppResult<EntryId> {
                        release.await()
                        return AppResult.Success(entry.id)
                    }
                }
            val model = createViewModel(createEntryUseCase(repository), AuthenticatedSession)
            model.onAction(AddOtpAction.TitleChanged("Submitted"))
            model.onAction(AddOtpAction.SecretChanged("JBSWY3DPEHPK3PXP"))
            model.onAction(AddOtpAction.Save)
            runCurrent()
            assertFalse(model.tryApplyScannedConfig(OtpConfig(secret = "MY")))
            assertEquals("JBSWY3DPEHPK3PXP", model.uiState.value.form.secret)
            val events = mutableListOf<Unit>()
            backgroundScope.launch { model.uriAppliedFeedback.collect { events += it } }
            runCurrent()
            assertTrue(events.isEmpty())
            release.complete(Unit)
            runCurrent()
        }

    @Test
    fun oldParseFeedbackDoesNotSurviveLockAndReauthentication() =
        runTest {
            val session = MutableSession()
            val model = createViewModel(createEntryUseCase(session = session), session)
            model.onAction(AddOtpAction.UriChanged(VALID_URI))
            session.state.value = AuthenticationState.Locked
            runCurrent()
            session.state.value = AuthenticationState.Authenticated(2L)
            runCurrent()
            val events = mutableListOf<Unit>()
            backgroundScope.launch { model.uriAppliedFeedback.collect { events += it } }
            runCurrent()
            assertTrue(events.isEmpty())
        }

    @Test
    fun manualEditingInvalidatesBufferedParseFeedback() =
        runTest {
            val model = createViewModel()
            model.onAction(AddOtpAction.UriChanged(VALID_URI))
            model.onAction(AddOtpAction.TitleChanged("New draft"))
            val events = mutableListOf<Unit>()
            backgroundScope.launch { model.uriAppliedFeedback.collect { events += it } }
            runCurrent()
            assertTrue(events.isEmpty())
        }

    @Test
    fun onlyTheLatestUriResultIsDeliveredToALateCollector() =
        runTest {
            val model = createViewModel()
            model.onPageEntered()
            model.onAction(AddOtpAction.UriChanged(VALID_URI))
            model.onAction(AddOtpAction.UriChanged(VALID_URI))
            val events = mutableListOf<Unit>()
            backgroundScope.launch { model.uriAppliedFeedback.collect { events += it } }
            runCurrent()
            assertEquals(listOf(Unit), events)
        }

    @Test
    fun destroyedEditorDoesNotDeliverBufferedSuccessFeedback() =
        runTest {
            val model = createViewModel()
            model.onPageEntered()
            model.onAction(AddOtpAction.UriChanged(VALID_URI))
            ViewModelStore().apply {
                put("otp", model)
                clear()
            }
            val events = mutableListOf<Unit>()
            backgroundScope.launch { model.uriAppliedFeedback.collect { events += it } }
            runCurrent()
            assertTrue(events.isEmpty())
        }

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun fullEditLoadsOtpAndWritesUpdatedConfiguration() =
        runTest {
            val entry =
                Entry(
                    identity =
                        EntryIdentity(
                            EntryId("otp-edit"),
                            EntryType.OTP,
                            timestamps = EntryTimestamps(1),
                        ),
                    profile = EntryProfile("Token"),
                    secret =
                        EntrySecret(
                            credential =
                                com.aozijx.passly.domain.entry.model.credential.OtpCredential(
                                    OtpConfig(secret = null),
                                ),
                        ),
                )
            val secret = "JBSWY3DPEHPK3PXP"
            val fixture =
                EditableEntryUseCaseFixture(
                    entry,
                    setOf(SensitiveFieldKey.OTP_SECRET),
                    mapOf(SensitiveFieldKey.OTP_SECRET to secret),
                )
            var generationLoads = 0
            val otpRuntime =
                OtpCodeRuntime(
                    scope = backgroundScope,
                    otpRuntimePolicyReader = defaultOtpRuntimePolicyReader(),
                    codeGenerator = { _, _ -> OtpResult.Success("123456") },
                    loadOtpMaterial = {
                        generationLoads++
                        OtpGenerationMaterial(OtpConfig(secret = secret).toGenerationConfig(), secret.toByteArray())
                    },
                    currentTimeMillis = { 1_000L },
                )
            val model =
                AddOtpViewModel(
                    createEntryUseCase(session = fixture.access),
                    fixture.access,
                    editEntry = fixture.editor,
                    otpRuntime = otpRuntime,
                )

            val lease = otpRuntime.acquire(entry.id.value)
            runCurrent()
            assertEquals(1, generationLoads)
            model.loadExistingEntry(fixture.editableSeed(entry.id))
            advanceUntilIdle()
            assertEquals(EditorLoadPhase.Ready, model.uiState.value.loadPhase)
            assertEquals(secret, model.uiState.value.form.secret)

            model.onAction(AddOtpAction.IssuerChanged("Updated issuer"))
            model.onAction(AddOtpAction.Save)
            advanceUntilIdle()
            runCurrent()
            assertEquals(
                "Updated issuer",
                fixture.query.current
                    ?.secret
                    ?.otp
                    ?.config
                    ?.issuer,
            )
            assertEquals("a successful OTP edit invalidates and reloads active runtime material", 2, generationLoads)
            lease.close()
        }

    @Test
    fun invalidInputCannotPersistGuessedOtpParameters() =
        runTest {
            val valid = OtpFormState(title = "Mail", secret = "JBSWY3DPEHPK3PXP")
            val invalidForms =
                listOf(
                    valid.copy(period = "broken"),
                    valid.copy(period = ""),
                    valid.copy(digits = "broken"),
                    valid.copy(digits = "9"),
                    valid.copy(type = OtpType.HOTP, counter = "broken"),
                    valid.copy(type = OtpType.HOTP, counter = ""),
                    valid.copy(secret = "INVALID!"),
                )
            for (form in invalidForms) {
                val created = mutableListOf<Entry>()
                val repository =
                    object : EntryCommandRepository by NoOpEntryCommandRepository {
                        override suspend fun createEntry(entry: Entry): AppResult<EntryId> {
                            created += entry
                            return AppResult.Success(entry.id)
                        }
                    }
                val viewModel = createViewModel(createEntryUseCase(repository), AuthenticatedSession)
                viewModel.setForm(form)
                viewModel.onAction(AddOtpAction.Save)

                assertEquals(0, created.size)
                assertEquals(form, viewModel.uiState.value.form)
                assertTrue(!viewModel.uiState.value.canSave)
            }
        }

    @Test
    fun validManualInputPersistsChosenParametersOnceAndClearsSecret() =
        runTest {
            val created = mutableListOf<Entry>()
            val repository =
                object : EntryCommandRepository by NoOpEntryCommandRepository {
                    override suspend fun createEntry(entry: Entry): AppResult<EntryId> {
                        created += entry
                        return AppResult.Success(entry.id)
                    }
                }
            val viewModel = createViewModel(createEntryUseCase(repository), AuthenticatedSession)
            viewModel.setForm(
                OtpFormState(
                    title = "Mail",
                    secret = " my====== ",
                    digits = "7",
                    period = "60",
                ),
            )
            viewModel.onAction(AddOtpAction.Save)

            assertEquals(EditorSaveEffect.Saved, viewModel.effects.first())
            val config =
                requireNotNull(
                    created
                        .single()
                        .secret.otp
                        ?.config,
                )
            assertEquals("MY", config.secret)
            assertEquals(7, config.digits)
            assertEquals(60, config.periodSeconds)
            assertEquals("", viewModel.uiState.value.form.secret)
        }

    @Test
    fun encodedUriPopulatesTheEditorWithoutChangingPlusOrPercentCharacters() =
        runTest {
            val viewModel = createViewModel()
            viewModel.onPageEntered()

            viewModel.onAction(
                AddOtpAction.UriChanged(
                    "otpauth://totp/Example%3Aalice+tag%2540mail?secret=JBSWY3DPEHPK3PXP&issuer=Example",
                ),
            )

            assertEquals("alice+tag%40mail", viewModel.uiState.value.form.accountName)
            assertEquals("Example", viewModel.uiState.value.form.issuer)
            assertEquals(Unit, viewModel.uriAppliedFeedback.first())
        }

    @Test
    fun unsupportedUriDoesNotReplaceExistingCredentialsOrEmitSuccessFeedback() =
        runTest {
            val viewModel = createViewModel()
            viewModel.onPageEntered()
            viewModel.onAction(AddOtpAction.TitleChanged("Keep"))
            viewModel.onAction(AddOtpAction.SecretChanged("Existing"))

            viewModel.onAction(
                AddOtpAction.UriChanged(
                    "otpauth://totp/alice?secret=JBSWY3DPEHPK3PXP&algorithm=MD5",
                ),
            )

            assertEquals("Existing", viewModel.uiState.value.form.secret)
            assertEquals("Keep", viewModel.uiState.value.form.title)
            assertNull(withTimeoutOrNull(1) { viewModel.uriAppliedFeedback.first() })
        }

    @Test
    fun `parse feedback buffered by an exited editor is not delivered after reentry`() =
        runTest {
            val viewModel = createViewModel()
            val oldOwner = viewModel.onPageEntered()
            viewModel.onAction(AddOtpAction.UriChanged(VALID_URI))

            viewModel.onPageExited(oldOwner)
            viewModel.onPageEntered()

            assertNull(withTimeoutOrNull(1) { viewModel.uriAppliedFeedback.first() })
        }

    @Test
    fun scannedHotp_roundTripsThroughFormDraftAndMaterializer() {
        val viewModel = createViewModel()
        val config =
            OtpConfig(
                type = OtpType.HOTP,
                secret = "Zm9v",
                algorithm = OtpHashAlgorithm.SHA512,
                digits = 8,
                periodSeconds = null,
                counter = Long.MAX_VALUE,
                encoding = OtpSecretEncoding.BASE64,
                issuer = "Example",
                accountName = "ada@example.com",
            )

        assertTrue(viewModel.tryApplyScannedConfig(config))
        val state: AddOtpEditorUiState = viewModel.uiState.value
        val entry =
            EntryDraftMaterializer().materialize(
                state.form.toEntryDraft(),
                EntryIdentity(
                    id = EntryId("otp-1"),
                    type = EntryType.OTP,
                    timestamps = EntryTimestamps(123L),
                ),
            )

        val roundTripped = requireNotNull(entry.secret.otp?.config)
        assertEquals(config.type, roundTripped.type)
        assertEquals(config.secret, roundTripped.secret)
        assertEquals(config.algorithm, roundTripped.algorithm)
        assertEquals(config.digits, roundTripped.digits)
        assertNull(roundTripped.periodSeconds)
        assertEquals(config.counter, roundTripped.counter)
        assertEquals(config.encoding, roundTripped.encoding)
        assertEquals(config.issuer, roundTripped.issuer)
        assertEquals(config.accountName, roundTripped.accountName)
        assertFalse(state.isSaving)
    }

    @Test
    fun switchingFromSteamToHotpRestoresNumericDigits() {
        val viewModel = createViewModel()
        viewModel.onAction(AddOtpAction.TitleChanged("OTP"))
        viewModel.onAction(AddOtpAction.SecretChanged("MY"))
        viewModel.onAction(AddOtpAction.TypeChanged(OtpType.STEAM))
        assertEquals("5", viewModel.uiState.value.form.digits)
        viewModel.onAction(AddOtpAction.TypeChanged(OtpType.HOTP))
        assertEquals("6", viewModel.uiState.value.form.digits)
        assertTrue(viewModel.uiState.value.canSave)
    }

    private fun createEntryUseCase(
        repository: EntryCommandRepository = NoOpEntryCommandRepository,
        session: SecureSessionAccessState = AuthenticatedSession,
    ) = CreateEntryUseCase(
        entryCommandRepository = repository,
        secureSessionAccessState = session,
        materializer = EntryDraftMaterializer(),
        autofillEntryWriter = NoOpAutofillEntryWriter,
        logger = testLogger(),
    )

    private fun AddOtpViewModel.setForm(form: OtpFormState) {
        onAction(AddOtpAction.TitleChanged(form.title))
        onAction(AddOtpAction.IssuerChanged(form.issuer))
        onAction(AddOtpAction.AccountNameChanged(form.accountName))
        onAction(AddOtpAction.SecretChanged(form.secret))
        onAction(AddOtpAction.TypeChanged(form.type))
        onAction(AddOtpAction.AlgorithmChanged(form.algorithm))
        onAction(AddOtpAction.EncodingChanged(form.encoding))
        onAction(AddOtpAction.PeriodChanged(form.period))
        onAction(AddOtpAction.DigitsChanged(form.digits))
        onAction(AddOtpAction.CounterChanged(form.counter))
        onAction(AddOtpAction.UriChanged(form.uriText))
    }

    private object AuthenticatedSession : SecureSessionAccessState {
        override val authenticationState: StateFlow<AuthenticationState> =
            MutableStateFlow(AuthenticationState.Authenticated(1L))

        override fun isUnlocked() = true
    }

    private class MutableSession : SecureSessionAccessState {
        val state = MutableStateFlow<AuthenticationState>(AuthenticationState.Authenticated(1L))
        override val authenticationState: StateFlow<AuthenticationState> = state

        override fun isUnlocked() = state.value is AuthenticationState.Authenticated
    }

    private companion object {
        const val VALID_URI = "otpauth://totp/Example?secret=JBSWY3DPEHPK3PXP"
    }

    private object NoOpEntryCommandRepository : EntryCommandRepository {
        override suspend fun createEntry(entry: Entry) = AppResult.Success(entry.id)

        override suspend fun updateEntry(
            id: EntryId,
            expectedVersion: EntryVersion,
            changes: EntryUpdate,
        ) = AppResult.Success(Unit)

        override suspend fun moveToTrash(
            id: EntryId,
            expectedVersion: EntryVersion,
        ) = AppResult.Success(Unit)

        override suspend fun restoreEntry(
            id: EntryId,
            expectedVersion: EntryVersion,
        ) = AppResult.Success(Unit)

        override suspend fun deletePermanently(
            id: EntryId,
            expectedVersion: EntryVersion,
        ) = AppResult.Success(Unit)

        override suspend fun emptyTrash() = AppResult.Success(0)
    }
}
