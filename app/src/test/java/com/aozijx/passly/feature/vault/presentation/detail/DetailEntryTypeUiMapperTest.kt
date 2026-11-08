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
import com.aozijx.passly.feature.vault.presentation.detail.model.DetailPasskeyUiModel
import com.aozijx.passly.feature.vault.presentation.detail.model.DetailSeedPhraseUiModel
import com.aozijx.passly.feature.vault.presentation.detail.model.ProtectedDetailFieldUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DetailEntryTypeUiMapperTest {
    @Test
    fun `identity number maps absent hidden and revealed states`() {
        val entry = entry(EntryType.ID_CARD, IdentityCredential(), username = "identity-user")

        assertEquals(
            ProtectedDetailFieldUiState.Absent,
            detailIdentityUiModel(entry, DetailFieldProjection(DetailUiState())).idNumber,
        )
        assertEquals(
            ProtectedDetailFieldUiState.Hidden,
            detailIdentityUiModel(
                entry,
                DetailFieldProjection(DetailUiState(sensitiveFieldKeys = setOf(SensitiveFieldKey.IDENTITY_NUMBER))),
            ).idNumber,
        )
        assertEquals(
            ProtectedDetailFieldUiState.Revealed("123-45"),
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

        assertEquals("fingerprint", actual.fingerprint)
        assertEquals(ProtectedDetailFieldUiState.Hidden, actual.passphrase)
        assertEquals(ProtectedDetailFieldUiState.Revealed("private-key"), actual.privateKey)
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
            DetailPasskeyUiModel(ProtectedDetailFieldUiState.Hidden, "YubiKey"),
            detailPasskeyUiModel(hiddenEntry, DetailFieldProjection(hiddenState)),
        )

        val revealedState =
            hiddenState.copy(
                revealedFields = mapOf(FieldKey.PASSKEY_DATA to TestSensitiveValue("private-reference")),
            )
        assertEquals(
            ProtectedDetailFieldUiState.Revealed("private-reference"),
            detailPasskeyUiModel(hiddenEntry, DetailFieldProjection(revealedState)).data,
        )
        assertNull(
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
    ) =
        Entry(
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
