package com.aozijx.passly.feature.vault.presentation.detail

import com.aozijx.passly.R
import com.aozijx.passly.domain.entry.model.Entry
import com.aozijx.passly.domain.entry.model.EntryAssociations
import com.aozijx.passly.domain.entry.model.EntryIcon
import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.entry.model.EntryIdentity
import com.aozijx.passly.domain.entry.model.EntryProfile
import com.aozijx.passly.domain.entry.model.EntrySecret
import com.aozijx.passly.domain.entry.model.EntryTimestamps
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.model.otp.OtpType
import com.aozijx.passly.domain.entry.model.EntryVersion
import com.aozijx.passly.domain.entry.model.FieldKey
import com.aozijx.passly.domain.entry.model.activity.ActivityType
import com.aozijx.passly.domain.entry.model.activity.EntryActivity
import com.aozijx.passly.domain.entry.model.credential.CardCredential
import com.aozijx.passly.domain.entry.model.credential.EntryCredential
import com.aozijx.passly.domain.entry.model.credential.IdentityCredential
import com.aozijx.passly.domain.entry.model.credential.LoginCredential
import com.aozijx.passly.domain.entry.model.credential.OtpCredential
import com.aozijx.passly.domain.entry.model.credential.PasskeyCredential
import com.aozijx.passly.domain.entry.model.credential.SshCredential
import com.aozijx.passly.domain.entry.model.credential.WifiCredential
import com.aozijx.passly.domain.entry.model.otp.OtpConfig
import com.aozijx.passly.domain.entry.model.relation.EntryLinkId
import com.aozijx.passly.domain.entry.model.relation.RelatedPasswordEntry
import com.aozijx.passly.domain.entry.model.sensitive.SensitiveFieldKey
import com.aozijx.passly.domain.entry.policy.EntryTagValidationError
import com.aozijx.passly.domain.sensitive.OwnedChars
import com.aozijx.passly.feature.vault.otp.OtpCodeState
import com.aozijx.passly.feature.vault.presentation.detail.DetailActivityHistoryState
import com.aozijx.passly.feature.vault.presentation.detail.DetailSnapshotRefreshPhase
import com.aozijx.passly.feature.vault.presentation.detail.DetailTagChipsUiState
import com.aozijx.passly.feature.vault.presentation.detail.TagEditorValidationUiState
import com.aozijx.passly.feature.vault.presentation.detail.history.DetailRevisionListState
import com.aozijx.passly.feature.vault.presentation.detail.history.DetailRevisionState
import com.aozijx.passly.feature.vault.presentation.detail.model.CredentialFieldUiState
import com.aozijx.passly.feature.vault.presentation.detail.model.DetailAssociatedDomainUiState
import com.aozijx.passly.feature.vault.presentation.detail.model.DetailAssociatedDomainValueUiState
import com.aozijx.passly.feature.vault.presentation.detail.model.DetailBankCardEditorUiState
import com.aozijx.passly.feature.vault.presentation.detail.model.DetailBankCardFieldUiModel
import com.aozijx.passly.feature.vault.presentation.detail.model.DetailBodySectionUiModel
import com.aozijx.passly.feature.vault.presentation.detail.model.DetailHistoryDescriptionUiState
import com.aozijx.passly.feature.vault.presentation.detail.model.DetailHistoryIcon
import com.aozijx.passly.feature.vault.presentation.detail.model.DetailHistorySheetUiModel
import com.aozijx.passly.feature.vault.presentation.detail.model.DetailNotesUiState
import com.aozijx.passly.feature.vault.otp.presentation.OtpUiState
import com.aozijx.passly.feature.vault.otp.presentation.displayCodeOrPlaceholder
import com.aozijx.passly.feature.vault.otp.presentation.toOtpUiState
import com.aozijx.passly.feature.vault.presentation.detail.model.DetailPageUiState
import com.aozijx.passly.feature.vault.presentation.detail.model.DetailRelatedEntrySectionUiState
import com.aozijx.passly.feature.vault.presentation.detail.model.DetailRelatedPasswordSectionUiState
import com.aozijx.passly.feature.vault.presentation.detail.model.DetailTitleUiState
import com.aozijx.passly.feature.vault.presentation.detail.model.RelatedPasswordRowUnlinkUiState
import com.aozijx.passly.feature.vault.presentation.detail.section.DetailSectionKey
import com.aozijx.passly.feature.vault.presentation.detail.section.DetailSectionResolver
import com.aozijx.passly.presentation.shared.entryvisual.VaultItemAssociatedAppUiState
import com.aozijx.passly.presentation.shared.entryvisual.VaultItemIconGlyphUiState
import com.aozijx.passly.presentation.shared.entryvisual.VaultItemIconImageUiState
import com.aozijx.passly.presentation.shared.entryvisual.VaultItemIconTintUiState
import com.aozijx.passly.presentation.shared.media.LocalIconImageModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DetailUiMapperTest {
    @Test
    fun loadingSnapshotMapsToAnExplicitPageState() {
        assertEquals(DetailPageUiState.Loading, toDetailPageUiState(DetailUiState()))
    }

    @Test
    fun bankCardDoesNotUseProfileUsernameAsCardholder() {
        val entry =
            Entry(
                identity = EntryIdentity(id = EntryId("card"), type = EntryType.BANK_CARD, timestamps = EntryTimestamps(1L)),
                profile = EntryProfile(title = "Card", username = "profile-user"),
                secret = EntrySecret(credential = CardCredential(cardHolder = " \t")),
            )
        val state = readyState(entry).copy(sections = listOf(DetailSectionKey.BANK_CARD))
        val card =
            content(state)
                .content.sections
                .filterIsInstance<DetailBodySectionUiModel.BankCard>()
                .single()
                .state

        assertEquals(
            "",
            card.fields
                .filterIsInstance<DetailBankCardFieldUiModel.Text>()
                .single { it.field == DetailField.CARDHOLDER }
                .value,
        )
    }

    @Test
    fun associatedDomainMapsMissingValueAndEditDraftToExplicitStates() {
        val missing = content(readyState(noteEntry())).content.associations.domain

        assertEquals(
            DetailAssociatedDomainUiState.Display(DetailAssociatedDomainValueUiState.NotSet),
            missing,
        )

        val entry =
            noteEntry().copy(
                profile =
                    EntryProfile(
                        title = "Example",
                        associations = EntryAssociations(primaryUrl = "https://example.com"),
                    ),
            )
        val display = content(readyState(entry)).content.associations.domain
        val editing =
            readyState(entry).copy(
                fieldEdits =
                    DetailFieldEditState()
                        .start(DetailDraftKey.Domain, "https://example.com")
                        .update(DetailDraftKey.Domain, "https://edited.example.com"),
            )

        assertEquals(
            DetailAssociatedDomainUiState.Display(
                DetailAssociatedDomainValueUiState.Value("https://example.com"),
            ),
            display,
        )
        assertEquals(
            DetailAssociatedDomainUiState.Editing("https://edited.example.com"),
            content(editing).content.associations.domain,
        )
    }

    @Test
    fun detailIconUsesSharedExplicitProjection() {
        val entry =
            noteEntry().copy(
                profile =
                    EntryProfile(
                        title = "Mail",
                        associations = EntryAssociations(applicationIds = setOf("com.example.mail")),
                        icon = EntryIcon(name = "communication.email", customReference = "icons/mail.png", color = "blue"),
                    ),
            )

        val icon = content(readyState(entry)).content.icon.icon

        assertEquals(VaultItemIconImageUiState.Custom(LocalIconImageModel("file://icons/mail.png")), icon.image)
        val glyph = icon.glyph as VaultItemIconGlyphUiState.Named
        assertEquals("communication.email", glyph.name)
        assertEquals(VaultItemIconTintUiState.StorageToken("blue"), glyph.tint)
        assertEquals(VaultItemAssociatedAppUiState.Package("com.example.mail"), icon.associatedApp)
    }

    @Test
    fun `installed app mapping preserves display label and package identity`() {
        val app =
            DetailInstalledApp(
                label = "Browser",
                packageName = "com.example.browser",
            )

        val actual = app.toPackagePickerItemUiModel()

        assertEquals("Browser", actual.label)
        assertEquals("com.example.browser", actual.packageName)
    }

    @Test
    fun `tag editor state exposes display chips and both validation limits`() {
        val scenarios =
            listOf(
                EntryTagValidationError.TooMany(maximum = 20) to
                    TagEditorValidationUiState.Invalid(
                        pluralResourceId = R.plurals.vault_detail_tag_too_many,
                        quantity = 20,
                    ),
                EntryTagValidationError.TooLong(maximum = 32) to
                    TagEditorValidationUiState.Invalid(
                        pluralResourceId = R.plurals.vault_detail_tag_too_long,
                        quantity = 32,
                    ),
            )

        scenarios.forEach { (validationError, expectedMessage) ->
            val state =
                DetailTagEditorState.Open(
                    initialTags = setOf("Work"),
                    draftTags = setOf("Personal"),
                    validation = validationError.toTagEditorValidationUiState(),
                )

            assertTrue(state.dirty)
            assertEquals(DetailTagChipsUiState.Items(listOf("Personal")), state.draftTagsUiState)
            assertEquals(expectedMessage, state.validation)
        }
    }

    @Test
    fun headerMapperKeepsOnlyHeaderState() {
        val note = noteEntry()
        val entry =
            note.copy(
                identity = note.identity.copy(type = EntryType.LOGIN),
                secret = EntrySecret(credential = LoginCredential()),
            )
        val state =
            readyState(entry).copy(
                fieldEdits = DetailFieldEditState().start(DetailDraftKey.Title, "Edited"),
            )

        val ui = detailHeaderUiModel(entry, state)

        assertEquals(DetailTitleUiState.Editing("Edited"), ui.title)
        assertTrue(ui.favorite)
    }

    @Test
    fun headerMapperUsesRegisteredFullFormEditCapability() {
        val note = noteEntry()
        val login =
            note.copy(
                identity = note.identity.copy(id = EntryId("login"), type = EntryType.LOGIN),
                secret = EntrySecret(credential = LoginCredential()),
            )

        assertFalse(detailHeaderUiModel(note, readyState(note)).canEditFullForm)
        assertTrue(detailHeaderUiModel(login, readyState(login)).canEditFullForm)
    }

    @Test
    fun `only active saved login entries expose password binding`() {
        val login =
            noteEntry().copy(
                identity = noteEntry().identity.copy(type = EntryType.LOGIN),
                secret = EntrySecret(credential = LoginCredential()),
            )
        val deletedLogin =
            login.copy(
                identity =
                    login.identity.copy(
                        timestamps = login.timestamps.copy(deletedAtMs = 5L),
                    ),
            )

        assertEquals(
            DetailRelatedPasswordSectionUiState.EmptyCanBind,
            content(readyState(login)).content.relatedSections.passwords,
        )
        assertEquals(
            DetailRelatedPasswordSectionUiState.Hidden,
            content(readyState(noteEntry())).content.relatedSections.passwords,
        )
        assertEquals(
            DetailRelatedPasswordSectionUiState.Hidden,
            content(readyState(deletedLogin)).content.relatedSections.passwords,
        )
    }

    @Test
    fun snapshotRefreshPhaseReachesDetailContent() {
        val state =
            DetailUiState(snapshotState = DetailSnapshotState.Content(noteEntry(), DetailSnapshotRefreshPhase.Failed))

        assertEquals(
            DetailSnapshotRefreshPhase.Failed,
            content(state).content.snapshotRefreshPhase,
        )
    }

    @Test
    fun presentationMapperProjectsRelationsMetadataAndActivity() {
        val entry = noteEntry()
        val state =
            readyState(entry).copy(
                accountGroupEntries = listOf(entry.copy(identity = entry.identity.copy(id = EntryId("account-member")))),
                otherRelatedEntries = listOf(entry.copy(identity = entry.identity.copy(id = EntryId("other-relation")))),
                relatedPasswords =
                    listOf(
                        RelatedPasswordEntry(
                            linkId = EntryLinkId("related-link"),
                            targetEntryId = EntryId("related-password"),
                            title = "Related password",
                            username = "person@example.test",
                        ),
                    ),
                passwordUnlink =
                    RelatedPasswordUnlinkUiState.Open(
                        target = RelatedPasswordUnlinkTargetUiModel("related-password", "related-link", "Related password"),
                        phase = RelatedPasswordUnlinkPhase.Removing,
                    ),
                activityHistory =
                    DetailActivityHistoryState.Content(
                        phase = DetailActivityHistoryPhase.Ready,
                        activities =
                            listOf(
                                EntryActivity(
                                    entryId = entry.id.value,
                                    activityType = ActivityType.AUTOFILL,
                                    createdAt = 3L,
                                ),
                            ),
                    ),
                revisions =
                    DetailRevisionState.Open(
                        entryId = entry.id,
                        revisions = DetailRevisionListState.Ready(emptyList()),
                    ),
            )

        val presentation = content(state)
        val content = presentation.content
        val relatedEntries = content.relatedSections.entries as DetailRelatedEntrySectionUiState.Items
        val relatedPasswords = content.relatedSections.passwords as DetailRelatedPasswordSectionUiState.Items
        val history = presentation.overlays.history as DetailHistorySheetUiModel.ListPage
        val activity = history.content.items.single()

        assertEquals(entry.id.value, presentation.entryId)
        assertEquals(listOf("account-member", "other-relation"), relatedEntries.entries.map { it.id })
        assertEquals("related-link", relatedPasswords.entries.single().linkId)
        assertEquals("related-password", relatedPasswords.entries.single().targetEntryId)
        assertEquals("person@example.test", relatedPasswords.entries.single().username)
        assertEquals(
            RelatedPasswordRowUnlinkUiState.Removing,
            relatedPasswords.entries.single().unlinkActionState,
        )
        assertEquals(1L, content.metadata.createdAt)
        assertEquals(1L, content.metadata.updatedAt)
        assertEquals(DetailHistoryIcon.AUTOFILLED, activity.icon)
        assertEquals(
            DetailHistoryDescriptionUiState.Label(R.string.vault_detail_activity_autofill),
            activity.description,
        )
        assertEquals(3L, activity.createdAtMs)
    }

    @Test
    fun otpMapperPreservesVolatileOtpState() {
        val ui =
            OtpCodeState.TimedReady(
                code = "123456",
                nextCode = "654321",
                progress = 0.25f,
                windowEndMillis = 30_000L,
                periodSeconds = 30,
            ).toOtpUiState(OtpType.TOTP)

        require(ui is OtpUiState.TimedReady)
        assertEquals("123 456", ui.currentCode)
        assertEquals("123 456", ui.displayCodeOrPlaceholder)
        assertEquals(0.25f, ui.progress)
    }

    @Test
    fun missingOtpRuntimeStateMapsToAnExplicitPlaceholderAndInactiveProgress() {
        val ui = (null as OtpCodeState?).toOtpUiState(OtpType.TOTP)

        assertEquals(OtpUiState.NotRequested, ui)
        assertEquals("------", ui.displayCodeOrPlaceholder)
    }

    @Test
    fun scopedSensitiveTextRedactsToStringAndWipesTemporaryCopy() {
        val source = OwnedChars.fromString("secret-value")
        val scoped = source.asScopedSensitiveText()
        lateinit var borrowed: CharArray

        val length =
            scoped.useChars {
                borrowed = it
                assertEquals("secret-value", String(it))
                it.size
            }

        assertEquals(12, length)
        assertEquals("***", scoped.toString())
        assertTrue(borrowed.all { it == '\u0000' })
        assertFalse(source.isWiped)
        source.close()
    }

    @Test
    fun presentationMapperProjectsEditorOwnershipAndSaveProgress() {
        val entry = noteEntry()
        val state =
            readyState(entry).copy(
                fieldEdits = DetailFieldEditState().start(DetailDraftKey.Notes, "draft"),
                saveState = DetailSaveState.Saving(DetailEditCompletion.Tags),
            )

        val presentation = content(state)

        assertEquals(DetailNotesUiState.Editing("draft"), presentation.content.notes)
        assertTrue(presentation.overlays.savingTags)
        assertFalse(presentation.overlays.savingIcon)
    }

    @Test
    fun notesMapContentAndAbsenceToExplicitStates() {
        assertEquals(DetailNotesUiState.Markdown("note"), content(readyState(noteEntry())).content.notes)

        val withoutNotes = noteEntry().copy(secret = EntrySecret())

        assertEquals(DetailNotesUiState.NotSet, content(readyState(withoutNotes)).content.notes)
    }

    @Test
    fun credentialFieldsMapHiddenRevealedAndEditingValuesToRenderStates() {
        val entry =
            Entry(
                identity = EntryIdentity(EntryId("login"), EntryType.LOGIN, timestamps = EntryTimestamps(1L)),
                profile = EntryProfile(title = "Login"),
                secret = EntrySecret(credential = LoginCredential(password = "stored-secret")),
            )
        val passwordField =
            readyState(entry).copy(
                sections = listOf(DetailSectionKey.CREDENTIAL),
                sensitiveFieldKeys = setOf(SensitiveFieldKey.PASSWORD),
            )
        val hidden = credentialFields(passwordField).single { it.field == DetailField.PASSWORD }
        assertEquals(CredentialFieldUiState.Hidden(DetailField.PASSWORD, R.string.password_label), hidden)

        val revealedValue = OwnedChars.fromString("revealed-secret")
        val revealedState = passwordField.copy(revealedFields = mapOf(FieldKey.PASSWORD to revealedValue))
        val revealed = credentialFields(revealedState).single { it.field == DetailField.PASSWORD }
        assertTrue(revealed is CredentialFieldUiState.Revealed)
        assertEquals(
            "revealed-secret",
            (revealed as CredentialFieldUiState.Revealed).value.useChars(::String),
        )
        val editingState =
            revealedState.copy(
                fieldEdits =
                    DetailFieldEditState()
                        .start(DetailDraftKey.EntryField(FieldKey.PASSWORD), "revealed-secret")
                        .update(DetailDraftKey.EntryField(FieldKey.PASSWORD), "changed-secret"),
            )
        assertEquals(
            CredentialFieldUiState.Editing(
                DetailField.PASSWORD,
                R.string.password_label,
                draft = "changed-secret",
                showModifiedHint = true,
            ),
            credentialFields(editingState).single { it.field == DetailField.PASSWORD },
        )
        revealedValue.close()
    }

    @Test
    fun presentationMapperProjectsRenderableSectionsInResolvedOrder() {
        val state =
            readyState(noteEntry()).copy(
                sections =
                    listOf(
                        DetailSectionKey.CREDENTIAL,
                        DetailSectionKey.ENTRY_TYPE,
                        DetailSectionKey.ASSOCIATED_INFO,
                        DetailSectionKey.NOTES,
                        DetailSectionKey.METADATA,
                        DetailSectionKey.ACTIVITY,
                    ),
            )

        val presentation = content(state)

        assertEquals(
            listOf(DetailBodySectionUiModel.Credential::class),
            presentation.content.sections.map { it::class },
        )
    }

    @Test
    fun presentationMapperEmitsEveryResolvedSectionAsExplicitPayload() {
        val cases =
            listOf(
                typedEntry(EntryType.LOGIN, LoginCredential()) to DetailBodySectionUiModel.Credential::class,
                typedEntry(EntryType.OTP, OtpCredential(OtpConfig(secret = "JBSWY3DPEHPK3PXP"))) to DetailBodySectionUiModel.Otp::class,
                typedEntry(EntryType.BANK_CARD, CardCredential()) to DetailBodySectionUiModel.BankCard::class,
                typedEntry(EntryType.ID_CARD, IdentityCredential(idNumber = "id")) to DetailBodySectionUiModel.Identity::class,
                typedEntry(EntryType.SSH_KEY, SshCredential(publicKey = "public")) to DetailBodySectionUiModel.Ssh::class,
                typedEntry(EntryType.SEED_PHRASE, IdentityCredential(seedPhrase = "seed")) to DetailBodySectionUiModel.SeedPhrase::class,
                typedEntry(EntryType.PASSKEY, PasskeyCredential(credentialId = "credential")) to DetailBodySectionUiModel.Passkey::class,
            )

        cases.forEach { (entry, expectedSection) ->
            val state = readyState(entry).copy(sections = DetailSectionResolver.resolve(entry))
            val sections = content(state).content.sections

            assertEquals(entry.type.name, listOf(expectedSection), sections.map { it::class })
        }
    }

    @Test
    fun `wifi presentation uses ssid as common title and common password field`() {
        val entry =
            Entry(
                identity =
                    EntryIdentity(
                        id = EntryId("wifi"),
                        type = EntryType.WIFI,
                        timestamps = EntryTimestamps(1L),
                    ),
                profile = EntryProfile(title = "Legacy title"),
                secret =
                    EntrySecret(
                        credential = WifiCredential(ssid = "Office", password = "wifi-secret"),
                    ),
            )
        val state =
            readyState(entry).copy(
                sections = listOf(DetailSectionKey.CREDENTIAL),
            )

        val presentation = content(state)

        assertEquals(DetailTitleUiState.Display("Office"), presentation.header.title)
        val sections = presentation.content.sections
        assertEquals(
            listOf(DetailBodySectionUiModel.Credential::class, DetailBodySectionUiModel.WifiQr::class),
            sections.map { it::class },
        )
        val credential = (sections.first() as DetailBodySectionUiModel.Credential).fields
        assertEquals(1, credential.size)
        assertEquals(DetailField.PASSWORD, credential.single().field)
        assertEquals(R.string.password_label, credential.single().titleRes)
    }

    @Test
    fun `bank card presentation keeps stored sensitive fields visible while locked`() {
        val entry =
            Entry(
                identity =
                    EntryIdentity(
                        id = EntryId("bank-card"),
                        type = EntryType.BANK_CARD,
                        timestamps = EntryTimestamps(1L),
                    ),
                profile = EntryProfile(title = "Daily card"),
                secret =
                    EntrySecret(
                        credential =
                            CardCredential(
                                cardHolder = "Alice",
                                cardExpiry = "08/2030",
                                issuingBank = "Bank",
                                bankBranch = "Branch",
                                cardProduct = "Product",
                                currency = "CNY / USD",
                                bankPhone = "+86 123456",
                            ),
                    ),
            )
        val state =
            readyState(entry).copy(
                sections = listOf(DetailSectionKey.BANK_CARD),
                sensitiveFieldKeys =
                    setOf(
                        SensitiveFieldKey.CARD_NUMBER,
                        SensitiveFieldKey.CARD_CVV,
                        SensitiveFieldKey.CARD_PAYMENT_PIN,
                    ),
            )

        val card =
            content(state)
                .content.sections
                .filterIsInstance<DetailBodySectionUiModel.BankCard>()
                .single()
                .state

        val textFields = card.fields.filterIsInstance<DetailBankCardFieldUiModel.Text>().associate { it.field to it.value }
        assertEquals("Alice", textFields[DetailField.CARDHOLDER])
        val bankDetails = textFields
        assertEquals("Bank", bankDetails[DetailField.CARD_ISSUING_BANK])
        assertEquals("Branch", bankDetails[DetailField.CARD_BANK_BRANCH])
        assertEquals("Product", bankDetails[DetailField.CARD_PRODUCT])
        assertEquals("CNY / USD", bankDetails[DetailField.CARD_CURRENCY])
        assertEquals("+86 123456", bankDetails[DetailField.CARD_BANK_PHONE])
        assertTrue(card.fields.any { it.field == DetailField.CARD_NUMBER })
        assertTrue(card.fields.any { it.field == DetailField.CARD_CVV })
        assertTrue(card.fields.any { it.field == DetailField.PAYMENT_PIN })
        assertEquals(
            DetailBankCardEditorUiState.ReadOnly,
            card.fields
                .filterIsInstance<DetailBankCardFieldUiModel.Protected>()
                .single { it.field == DetailField.CARD_NUMBER }
                .editor,
        )
        assertTrue(card.canRevealMore)

        val revealed =
            content(
                state.copy(
                    revealedFields = mapOf(FieldKey.CARD_NUMBER to OwnedChars.fromString("4111111111111111")),
                ),
            ).content.sections.filterIsInstance<DetailBodySectionUiModel.BankCard>().single().state
        assertEquals(
            DetailBankCardEditorUiState.Available,
            revealed.fields
                .filterIsInstance<DetailBankCardFieldUiModel.Protected>()
                .single { it.field == DetailField.CARD_NUMBER }
                .editor,
        )
    }

    private fun noteEntry() =
        Entry(
            identity =
                EntryIdentity(
                    id = EntryId("entry"),
                    type = EntryType.NOTE,
                    timestamps = EntryTimestamps(1L),
                ),
            profile = EntryProfile(title = "Example", username = "user", favorite = true),
            secret = EntrySecret(notes = "note"),
        )

    private fun typedEntry(
        type: EntryType,
        credential: EntryCredential,
    ) =
        Entry(
            identity =
                EntryIdentity(
                    id = EntryId("detail-${type.name}"),
                    type = type,
                    version = EntryVersion.INITIAL,
                    timestamps = EntryTimestamps(1L),
                ),
            profile = EntryProfile(title = type.name),
            secret = EntrySecret(credential = credential),
        )

    private fun readyState(entry: Entry) = DetailUiState(snapshotState = DetailSnapshotState.Content(entry))

    private fun credentialFields(state: DetailUiState) =
        content(state)
            .content.sections
            .filterIsInstance<DetailBodySectionUiModel.Credential>()
            .single()
            .fields

    private fun content(state: DetailUiState) = (toDetailPageUiState(state) as DetailPageUiState.Content).presentation
}
