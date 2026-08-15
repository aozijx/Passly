package com.aozijx.passly.domain.settings.model

/** 设置快照聚合；子设置模型按域分布在同包的 Appearance/Interface/Security/Interaction/Library/Backup 文件中。 */
data class AppSettingsSnapshot(
    val appearance: AppearanceSettings,
    val interfacePrefs: InterfaceSettings,
    val security: SecuritySettings,
    val interaction: InteractionSettings,
    val messages: MessageSettings,
    val vault: LibraryViewSettings,
    val backup: BackupSettings
)
