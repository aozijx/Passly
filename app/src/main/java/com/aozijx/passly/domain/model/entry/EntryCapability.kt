package com.aozijx.passly.domain.model.entry

/**
 * 条目具备的能力特征。
 *
 * 每个 [EntryType] 具有一组预定义的能力，用于 UI 展示、权限检查、
 * 行为开关等场景，避免在 Consumer 侧写 when 分支来判断能力。
 */
enum class EntryCapability {
    /** 包含可复制/自动填充的密码。 */
    HAS_PASSWORD,

    /** 包含 TOTP/Steam 凭据。 */
    HAS_OTP,

    /** 包含 SSH 私钥。 */
    HAS_SSH_KEY,

    /** 包含 Passkey。 */
    HAS_PASSKEY,

    /** 包含 Wi-Fi 凭据。 */
    HAS_WIFI,

    /** 包含身份信息（证件号、安全问题等）。 */
    HAS_IDENTITY,

    /** 包含支付卡片信息。 */
    HAS_CARD,

    /** 包含附件。 */
    HAS_ATTACHMENTS,

    /** 支持自定义字段。 */
    HAS_CUSTOM_FIELDS;

    companion object {
        /** 根据条目类型返回默认能力集合。 */
        fun capabilitiesFor(entryType: EntryType): Set<EntryCapability> = when (entryType) {
            EntryType.LOGIN -> setOf(HAS_PASSWORD, HAS_CUSTOM_FIELDS)
            EntryType.TOTP -> setOf(HAS_OTP)
            EntryType.CARD, EntryType.BANK_CARD -> setOf(HAS_CARD, HAS_CUSTOM_FIELDS)
            EntryType.IDENTITY, EntryType.ID_CARD -> setOf(HAS_IDENTITY, HAS_CUSTOM_FIELDS)
            EntryType.SSH_KEY -> setOf(HAS_SSH_KEY)
            EntryType.WIFI -> setOf(HAS_WIFI, HAS_PASSWORD)
            EntryType.PASSKEY -> setOf(HAS_PASSKEY)
            else -> setOf(HAS_CUSTOM_FIELDS)
        }
    }
}
