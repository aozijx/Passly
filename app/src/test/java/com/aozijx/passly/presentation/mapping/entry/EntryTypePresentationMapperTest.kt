package com.aozijx.passly.presentation.mapping.entry

import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.presentation.shared.entry.EntryTypeUiModel
import org.junit.Assert.assertEquals
import org.junit.Test

class EntryTypePresentationMapperTest {

    @Test
    fun `domain and ui entry types map exhaustively in both directions`() {
        val expected = mapOf(
            EntryType.ACCOUNT to EntryTypeUiModel.ACCOUNT,
            EntryType.LOGIN to EntryTypeUiModel.LOGIN,
            EntryType.NOTE to EntryTypeUiModel.NOTE,
            EntryType.BANK_CARD to EntryTypeUiModel.BANK_CARD,
            EntryType.ID_CARD to EntryTypeUiModel.ID_CARD,
            EntryType.PASSPORT to EntryTypeUiModel.PASSPORT,
            EntryType.DRIVER_LICENSE to EntryTypeUiModel.DRIVER_LICENSE,
            EntryType.SSH_KEY to EntryTypeUiModel.SSH_KEY,
            EntryType.WIFI to EntryTypeUiModel.WIFI,
            EntryType.PASSKEY to EntryTypeUiModel.PASSKEY,
            EntryType.OTP to EntryTypeUiModel.OTP,
            EntryType.DATABASE_CREDENTIAL to EntryTypeUiModel.DATABASE_CREDENTIAL,
            EntryType.SERVER_CREDENTIAL to EntryTypeUiModel.SERVER_CREDENTIAL,
            EntryType.API_KEY to EntryTypeUiModel.API_KEY,
            EntryType.CRYPTO_WALLET to EntryTypeUiModel.CRYPTO_WALLET,
            EntryType.SEED_PHRASE to EntryTypeUiModel.SEED_PHRASE,
            EntryType.RECOVERY_CODE to EntryTypeUiModel.RECOVERY_CODE,
        )

        assertEquals(EntryType.entries.toSet(), expected.keys)
        assertEquals(EntryTypeUiModel.entries.toSet(), expected.values.toSet())
        expected.forEach { (domain, ui) ->
            assertEquals(ui, domain.toUiModel())
            assertEquals(domain, ui.toDomainModel())
        }
    }
}
