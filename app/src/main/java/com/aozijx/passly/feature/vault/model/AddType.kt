package com.aozijx.passly.feature.vault.model

import com.aozijx.passly.domain.entry.model.EntryType

/**
 * 保险箱新增操作类型。
 *
 * 无操作状态用可空类型 `AddType?` 表达（`null` 即未触发任何新增动作）。
 */
enum class AddType {
    PASSWORD,
    TOTP,
    BANK_CARD,
    WIFI,
    SSH_KEY,
    ID_CARD,
    SEED_PHRASE,
    PASSKEY,
    RECOVERY_CODE;

    companion object {
        /** FAB 快捷菜单只保留直接创建的常用条目。 */
        val fabMenuOptions: List<AddType> = listOf(TOTP, PASSWORD)

        /** ModalBottomSheet 中显示的所有添加类型 */
        val allOptions: List<AddType> = entries.toList()
    }
}

val AddType.entryTypes: Set<EntryType>
    get() = when (this) {
        AddType.PASSWORD -> setOf(
            EntryType.LOGIN,
            EntryType.DATABASE_CREDENTIAL,
            EntryType.SERVER_CREDENTIAL,
            EntryType.API_KEY,
            EntryType.CRYPTO_WALLET,
        )
        AddType.TOTP -> setOf(EntryType.OTP)
        AddType.BANK_CARD -> setOf(EntryType.BANK_CARD)
        AddType.WIFI -> setOf(EntryType.WIFI)
        AddType.SSH_KEY -> setOf(EntryType.SSH_KEY)
        AddType.ID_CARD -> setOf(EntryType.ID_CARD, EntryType.PASSPORT, EntryType.DRIVER_LICENSE)
        AddType.SEED_PHRASE -> setOf(EntryType.SEED_PHRASE)
        AddType.PASSKEY -> setOf(EntryType.PASSKEY)
        AddType.RECOVERY_CODE -> setOf(EntryType.RECOVERY_CODE)
    }
