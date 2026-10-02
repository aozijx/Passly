package com.aozijx.passly.presentation.mapping.entry

import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.presentation.shared.entry.EntryTypeUiModel

internal fun EntryType.toUiModel(): EntryTypeUiModel = when (this) {
    EntryType.ACCOUNT -> EntryTypeUiModel.ACCOUNT
    EntryType.LOGIN -> EntryTypeUiModel.LOGIN
    EntryType.NOTE -> EntryTypeUiModel.NOTE
    EntryType.BANK_CARD -> EntryTypeUiModel.BANK_CARD
    EntryType.ID_CARD -> EntryTypeUiModel.ID_CARD
    EntryType.PASSPORT -> EntryTypeUiModel.PASSPORT
    EntryType.DRIVER_LICENSE -> EntryTypeUiModel.DRIVER_LICENSE
    EntryType.SSH_KEY -> EntryTypeUiModel.SSH_KEY
    EntryType.WIFI -> EntryTypeUiModel.WIFI
    EntryType.PASSKEY -> EntryTypeUiModel.PASSKEY
    EntryType.OTP -> EntryTypeUiModel.OTP
    EntryType.DATABASE_CREDENTIAL -> EntryTypeUiModel.DATABASE_CREDENTIAL
    EntryType.SERVER_CREDENTIAL -> EntryTypeUiModel.SERVER_CREDENTIAL
    EntryType.API_KEY -> EntryTypeUiModel.API_KEY
    EntryType.CRYPTO_WALLET -> EntryTypeUiModel.CRYPTO_WALLET
    EntryType.SEED_PHRASE -> EntryTypeUiModel.SEED_PHRASE
    EntryType.RECOVERY_CODE -> EntryTypeUiModel.RECOVERY_CODE
}

internal fun EntryTypeUiModel.toDomainModel(): EntryType = when (this) {
    EntryTypeUiModel.ACCOUNT -> EntryType.ACCOUNT
    EntryTypeUiModel.LOGIN -> EntryType.LOGIN
    EntryTypeUiModel.NOTE -> EntryType.NOTE
    EntryTypeUiModel.BANK_CARD -> EntryType.BANK_CARD
    EntryTypeUiModel.ID_CARD -> EntryType.ID_CARD
    EntryTypeUiModel.PASSPORT -> EntryType.PASSPORT
    EntryTypeUiModel.DRIVER_LICENSE -> EntryType.DRIVER_LICENSE
    EntryTypeUiModel.SSH_KEY -> EntryType.SSH_KEY
    EntryTypeUiModel.WIFI -> EntryType.WIFI
    EntryTypeUiModel.PASSKEY -> EntryType.PASSKEY
    EntryTypeUiModel.OTP -> EntryType.OTP
    EntryTypeUiModel.DATABASE_CREDENTIAL -> EntryType.DATABASE_CREDENTIAL
    EntryTypeUiModel.SERVER_CREDENTIAL -> EntryType.SERVER_CREDENTIAL
    EntryTypeUiModel.API_KEY -> EntryType.API_KEY
    EntryTypeUiModel.CRYPTO_WALLET -> EntryType.CRYPTO_WALLET
    EntryTypeUiModel.SEED_PHRASE -> EntryType.SEED_PHRASE
    EntryTypeUiModel.RECOVERY_CODE -> EntryType.RECOVERY_CODE
}

internal fun Set<EntryTypeUiModel>.toDomainModels(): Set<EntryType> =
    mapTo(linkedSetOf(), EntryTypeUiModel::toDomainModel)
