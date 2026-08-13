package com.aozijx.passly.feature.vault.model

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
