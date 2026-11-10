package com.aozijx.passly.feature.vault.presentation.detail

import com.aozijx.passly.R
import com.aozijx.passly.domain.entry.model.Entry
import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.entry.model.EntryIdentity
import com.aozijx.passly.domain.entry.model.EntryProfile
import com.aozijx.passly.domain.entry.model.EntrySecret
import com.aozijx.passly.domain.entry.model.EntryTimestamps
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.policy.EntryTagValidationError
import com.aozijx.passly.domain.entry.model.activity.ActivityType
import com.aozijx.passly.domain.entry.model.activity.EntryActivity
import com.aozijx.passly.domain.entry.model.activity.EntryActivityContext
import com.aozijx.passly.domain.entry.model.credential.CardCredential
import com.aozijx.passly.domain.entry.model.credential.LoginCredential
import com.aozijx.passly.domain.entry.model.credential.WifiCredential
import com.aozijx.passly.domain.entry.model.relation.EntryLinkId
import com.aozijx.passly.domain.entry.model.sensitive.SensitiveFieldKey
import com.aozijx.passly.domain.sensitive.OwnedChars
import com.aozijx.passly.domain.entry.model.relation.RelatedPasswordEntry
import com.aozijx.passly.feature.vault.model.OtpCodeState
import com.aozijx.passly.feature.vault.presentation.detail.DetailActivityHistoryState
import com.aozijx.passly.feature.vault.presentation.detail.section.DetailSectionKey
import com.aozijx.passly.feature.vault.presentation.detail.model.DetailActivityHistoryUiState
import com.aozijx.passly.feature.vault.presentation.detail.model.DetailBodySectionUiModel
import com.aozijx.passly.feature.vault.presentation.detail.model.DetailOtpUiModel
import com.aozijx.passly.feature.vault.presentation.detail.model.DetailPageUiState
import com.aozijx.passly.feature.vault.presentation.detail.model.DetailRelatedEntrySectionUiState
import com.aozijx.passly.feature.vault.presentation.detail.model.DetailRelatedPasswordSectionUiState
import com.aozijx.passly.feature.vault.presentation.detail.model.DetailSnapshotRefreshUiState
import com.aozijx.passly.feature.vault.presentation.detail.model.DetailTagChipsUiState
import com.aozijx.passly.feature.vault.presentation.detail.model.TagEditorValidationUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DetailUiMapperTest {
    @Test
    fun loadingSnapshotMapsToAnExplicitPageState() {
        assertEquals(DetailPageUiState.Loading, toDetailPageUiState(DetailUiState(), OtpCodeState.NotRequested))
    }

    @Test
    fun bankCardDoesNotUseProfileUsernameAsCardholder() {
        val entry =
            Entry(
                identity = EntryIdentity(id = EntryId("card"), type = EntryType.BANK_CARD, timestamps = EntryTimestamps(1L)),
                profile = EntryProfile(title = "Card", username = "profile-user"),
                secret = EntrySecret(credential = CardCredential()),
            )
        val state = readyState(entry).copy(sections = listOf(DetailSectionKey.BANK_CARD))
        val card =
            content(state)
                .content.sections.filterIsInstance<DetailBodySectionUiModel.BankCard>().single().state

        assertNull(card.cardholder)
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
        val entry = noteEntry()
        val state =
            readyState(entry).copy(
                fieldEdits = DetailFieldEditState().start(DetailDraftKey.Title, "Edited"),
            )

        val ui = detailHeaderUiModel(entry, state)

        assertEquals("Example", ui.title)
        assertTrue(ui.favorite)
        assertEquals("Edited", ui.editedTitle)
        assertTrue(ui.isEditingTitle)
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
                identity = login.identity.copy(
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
    fun snapshotRefreshStateReachesDetailContent() {
        val state =
            DetailUiState(snapshotState = DetailSnapshotState.RefreshFailedAfterRestore(noteEntry()))

        assertEquals(
            DetailSnapshotRefreshUiState.Failed,
            content(state).content.snapshotRefreshState,
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
                activityHistory =
                    DetailActivityHistoryState.Ready(
                        listOf(
                            EntryActivity(
                                entryId = entry.id.value,
                                activityType = ActivityType.AUTOFILL,
                                createdAt = 3L,
                            ),
                        ),
                    ),
            )

        val content = content(state).content
        val relatedEntries = content.relatedSections.entries as DetailRelatedEntrySectionUiState.Items
        val relatedPasswords = content.relatedSections.passwords as DetailRelatedPasswordSectionUiState.Items
        val activities = content.activityHistory as DetailActivityHistoryUiState.ReadyItems

        assertEquals(listOf("account-member", "other-relation"), relatedEntries.entries.map { it.id })
        assertEquals("related-link", relatedPasswords.entries.single().linkId)
        assertEquals("related-password", relatedPasswords.entries.single().targetEntryId)
        assertEquals("person@example.test", relatedPasswords.entries.single().username)
        assertEquals(1L, content.metadata.createdAt)
        assertEquals(1L, content.metadata.updatedAt)
        assertEquals(ActivityType.AUTOFILL, activities.activities.single().type)
        assertEquals(EntryActivityContext.Usage, activities.activities.single().context)
        assertEquals(3L, activities.activities.single().createdAt)
    }

    @Test
    fun otpMapperPreservesVolatileOtpState() {
        val ui =
            detailOtpUiModel(
                OtpCodeState.Ready(
                    code = "123456",
                    progress = 0.25f,
                ),
            )

        require(ui is DetailOtpUiModel.Ready)
        assertEquals("123456", ui.code)
        assertEquals("123 456", ui.displayCode)
        assertEquals(0.25f, ui.progress)
    }

    @Test
    fun otpNotRequestedStateProvidesExplicitPlaceholderAndInactiveProgress() {
        val ui = detailOtpUiModel(OtpCodeState.NotRequested)

        assertEquals(DetailOtpUiModel.NotRequested, ui)
        assertEquals("------", ui.displayCode)
        assertEquals(0f, ui.progress)
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

        assertTrue(presentation.content.notes.isEditing)
        assertEquals("draft", presentation.content.notes.editedNotes)
        assertTrue(presentation.overlays.savingTags)
        assertFalse(presentation.overlays.savingIcon)
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
        val state =
            readyState(noteEntry()).copy(
                sections =
                    listOf(
                        DetailSectionKey.CREDENTIAL,
                        DetailSectionKey.OTP,
                        DetailSectionKey.BANK_CARD,
                        DetailSectionKey.IDENTITY,
                        DetailSectionKey.SSH,
                        DetailSectionKey.SEED_PHRASE,
                        DetailSectionKey.PASSKEY,
                    ),
            )

        val sections = content(state).content.sections

        assertEquals(
            listOf(
                DetailBodySectionUiModel.Credential::class,
                DetailBodySectionUiModel.Otp::class,
                DetailBodySectionUiModel.BankCard::class,
                DetailBodySectionUiModel.Identity::class,
                DetailBodySectionUiModel.Ssh::class,
                DetailBodySectionUiModel.SeedPhrase::class,
                DetailBodySectionUiModel.Passkey::class,
            ),
            sections.map { it::class },
        )
        assertEquals(DetailOtpUiModel.NotRequested, (sections[1] as DetailBodySectionUiModel.Otp).state)
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

        assertEquals("Office", presentation.header.title)
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
                .content.sections.filterIsInstance<DetailBodySectionUiModel.BankCard>().single().state

        assertEquals("Alice", card.cardholder)
        val bankDetails = card.bankDetails.associate { it.field to it.value }
        assertEquals("Bank", bankDetails[DetailField.CARD_ISSUING_BANK])
        assertEquals("Branch", bankDetails[DetailField.CARD_BANK_BRANCH])
        assertEquals("Product", bankDetails[DetailField.CARD_PRODUCT])
        assertEquals("CNY / USD", bankDetails[DetailField.CARD_CURRENCY])
        assertEquals("+86 123456", bankDetails[DetailField.CARD_BANK_PHONE])
        assertTrue(card.cardNumber.isPresent)
        assertTrue(card.hasCvv)
        assertTrue(card.paymentPin.isPresent)
        assertTrue(card.canRevealMore)
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

    private fun readyState(entry: Entry) = DetailUiState(snapshotState = DetailSnapshotState.Ready(entry))

    private fun content(state: DetailUiState) =
        (toDetailPageUiState(state, OtpCodeState.NotRequested) as DetailPageUiState.Content).presentation
}
