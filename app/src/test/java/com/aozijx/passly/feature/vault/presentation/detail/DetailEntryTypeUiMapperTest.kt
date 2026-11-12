package com.aozijx.passly.feature.vault.presentation.detail

import com.aozijx.passly.domain.entry.model.Entry
import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.entry.model.EntryIdentity
import com.aozijx.passly.domain.entry.model.EntryProfile
import com.aozijx.passly.domain.entry.model.EntrySecret
import com.aozijx.passly.domain.entry.model.EntryTimestamps
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.model.FieldKey
import com.aozijx.passly.domain.entry.model.credential.EntryCredential
import com.aozijx.passly.domain.entry.model.credential.IdentityCredential
import com.aozijx.passly.domain.entry.model.credential.PasskeyCredential
import com.aozijx.passly.domain.entry.model.credential.SshCredential
import com.aozijx.passly.domain.entry.model.sensitive.SensitiveFieldKey
import com.aozijx.passly.domain.sensitive.SensitiveValue
import com.aozijx.passly.feature.vault.presentation.detail.model.DetailHardwareKeyInfoUiState
import com.aozijx.passly.feature.vault.presentation.detail.model.DetailIdentityNumberUiState
import com.aozijx.passly.feature.vault.presentation.detail.model.DetailPasskeyDataUiState
import com.aozijx.passly.feature.vault.presentation.detail.model.DetailPasskeyUiModel
import com.aozijx.passly.feature.vault.presentation.detail.model.DetailSeedPhraseUiModel
import com.aozijx.passly.feature.vault.presentation.detail.model.DetailSshFingerprintUiState
import com.aozijx.passly.feature.vault.presentation.detail.model.DetailSshPassphraseUiState
import com.aozijx.passly.feature.vault.presentation.detail.model.DetailSshPrivateKeyUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DetailEntryTypeUiMapperTest {
    @Test
    fun `identity number maps absent hidden and revealed states`() {
        val entry = entry(EntryType.ID_CARD, IdentityCredential(), username = "identity-user")

        assertEquals(
            DetailIdentityNumberUiState.Absent,
            detailIdentityUiModel(entry, DetailFieldProjection(DetailUiState())).idNumber,
        )
        assertEquals(
            DetailIdentityNumberUiState.Hidden,
            detailIdentityUiModel(
                entry,
                DetailFieldProjection(DetailUiState(sensitiveFieldKeys = setOf(SensitiveFieldKey.IDENTITY_NUMBER))),
            ).idNumber,
        )
        assertEquals(
            DetailIdentityNumberUiState.Revealed("123-45"),
            detailIdentityUiModel(
                entry,
                DetailFieldProjection(
                    DetailUiState(
                        sensitiveFieldKeys = setOf(SensitiveFieldKey.IDENTITY_NUMBER),
                        revealedFields = mapOf(FieldKey.ID_NUMBER to TestSensitiveValue("123-45")),
                    ),
                ),
            ).idNumber,
        )
        assertEquals("identity-user", detailIdentityUiModel(entry, DetailFieldProjection(DetailUiState())).username)
    }

    @Test
    fun `ssh mapper independently projects private key and passphrase`() {
        val entry = entry(EntryType.SSH_KEY, SshCredential(), username = "fingerprint")
        val state =
            DetailUiState(
                sensitiveFieldKeys = setOf(SensitiveFieldKey.SSH_PRIVATE_KEY, SensitiveFieldKey.SSH_PASSPHRASE),
                revealedFields = mapOf(FieldKey.SSH_KEY to TestSensitiveValue("private-key")),
            )

        val actual = detailSshUiModel(entry, state, DetailFieldProjection(state))

        assertEquals(DetailSshFingerprintUiState.Available("fingerprint"), actual.fingerprint)
        assertEquals(DetailSshPassphraseUiState.Hidden, actual.passphrase)
        assertEquals(DetailSshPrivateKeyUiState.Revealed("private-key"), actual.privateKey)
        assertEquals("private-key...", (actual.privateKey as DetailSshPrivateKeyUiState.Revealed).preview)
        assertTrue(actual.canRevealMore)
    }

    @Test
    fun `ssh mapper projects passphrase editing without losing reveal capability`() {
        val entry = entry(EntryType.SSH_KEY, SshCredential())
        val state =
            DetailUiState(
                sensitiveFieldKeys = setOf(SensitiveFieldKey.SSH_PASSPHRASE),
                fieldEdits =
                    DetailFieldEditState().start(
                        DetailDraftKey.EntryField(FieldKey.SSH_PASSPHRASE),
                        "draft",
                    ),
            )

        val actual = detailSshUiModel(entry, state, DetailFieldProjection(state))

        assertEquals(DetailSshPassphraseUiState.Editing("draft"), actual.passphrase)
        assertTrue(actual.canRevealMore)
    }

    @Test
    fun `seed phrase distinguishes absent hidden words and revealed empty value`() {
        assertEquals(
            DetailSeedPhraseUiModel.Absent,
            detailSeedPhraseUiModel(DetailFieldProjection(DetailUiState())),
        )
        assertEquals(
            DetailSeedPhraseUiModel.Hidden,
            detailSeedPhraseUiModel(
                DetailFieldProjection(DetailUiState(sensitiveFieldKeys = setOf(SensitiveFieldKey.SEED_PHRASE))),
            ),
        )
        assertEquals(
            DetailSeedPhraseUiModel.RevealedWords(listOf("one", "two", "three")),
            detailSeedPhraseUiModel(
                DetailFieldProjection(
                    DetailUiState(
                        sensitiveFieldKeys = setOf(SensitiveFieldKey.SEED_PHRASE),
                        revealedFields = mapOf(FieldKey.SEED_PHRASE to TestSensitiveValue("one two three")),
                    ),
                ),
            ),
        )
        assertEquals(
            DetailSeedPhraseUiModel.RevealedWithoutWords,
            detailSeedPhraseUiModel(
                DetailFieldProjection(
                    DetailUiState(
                        sensitiveFieldKeys = setOf(SensitiveFieldKey.SEED_PHRASE),
                        revealedFields = mapOf(FieldKey.SEED_PHRASE to TestSensitiveValue("   ")),
                    ),
                ),
            ),
        )
    }

    @Test
    fun `passkey mapper projects protected reference and trims optional hardware info`() {
        val hiddenEntry = entry(EntryType.PASSKEY, PasskeyCredential(hardwareKeyInfo = "  YubiKey  "))
        val hiddenState = DetailUiState(sensitiveFieldKeys = setOf(SensitiveFieldKey.PASSKEY_PRIVATE_REFERENCE))

        assertEquals(
            DetailPasskeyDataUiState.Absent,
            detailPasskeyUiModel(
                entry(EntryType.PASSKEY, PasskeyCredential()),
                DetailFieldProjection(DetailUiState()),
            ).data,
        )
        assertEquals(
            DetailPasskeyUiModel(
                data = DetailPasskeyDataUiState.Hidden,
                hardwareKeyInfo = DetailHardwareKeyInfoUiState.Available("YubiKey"),
            ),
            detailPasskeyUiModel(hiddenEntry, DetailFieldProjection(hiddenState)),
        )

        val revealedState =
            hiddenState.copy(
                revealedFields = mapOf(FieldKey.PASSKEY_DATA to TestSensitiveValue("private-reference")),
            )
        assertEquals(
            DetailPasskeyDataUiState.Revealed("private-reference"),
            detailPasskeyUiModel(hiddenEntry, DetailFieldProjection(revealedState)).data,
        )
        assertEquals(
            DetailHardwareKeyInfoUiState.NotSet,
            detailPasskeyUiModel(
                entry(EntryType.PASSKEY, PasskeyCredential(hardwareKeyInfo = "  ")),
                DetailFieldProjection(DetailUiState()),
            ).hardwareKeyInfo,
        )
    }

    private fun entry(
        type: EntryType,
        credential: EntryCredential,
        username: String = "",
    ) = Entry(
        identity = EntryIdentity(EntryId(type.name), type, timestamps = EntryTimestamps(1L)),
        profile = EntryProfile(title = type.name, username = username),
        secret = EntrySecret(credential = credential),
    )

    private class TestSensitiveValue(
        private val value: String,
    ) : SensitiveValue {
        override val isEmpty: Boolean get() = value.isEmpty()

        override fun toCharArray(): CharArray = value.toCharArray()

        override fun wipe() = Unit
    }
}
