package com.aozijx.passly.domain.settings.model

import com.aozijx.passly.domain.notice.model.AppMessageSettings

data class AppSettingsSnapshot(
    val appearance: AppearanceSettings,
    val interfacePrefs: InterfaceSettings,
    val security: SecuritySettings,
    val interaction: InteractionSettings,
    val messages: AppMessageSettings,
    val vault: VaultViewSettings,
    val backup: BackupSettings
)

// ============================================================
// 1. 外观
// ============================================================

data class AppearanceSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val isDynamicColor: Boolean = true,
    val fallbackPalette: FallbackPalette = FallbackPalette.BLUE,
    val customSeedArgb: Long? = null,
    val language: AppLanguage = AppLanguage.SYSTEM,
    val fontFamily: FontFamilyMode = FontFamilyMode.APP_BUNDLED
)

enum class ThemeMode { SYSTEM, LIGHT, DARK }
enum class FallbackPalette { BLUE, GREEN, RED, PURPLE, ORANGE, TEAL, PINK }
enum class AppLanguage { SYSTEM, ZH, EN, JA }
enum class FontFamilyMode { SYSTEM, APP_BUNDLED }

// ============================================================
// 2. 界面行为
// ============================================================

data class InterfaceSettings(
    val hideSystemBars: Boolean = false,
    val collapseTopBarOnScroll: Boolean = false,
    val collapseTabBarOnScroll: Boolean = false
)

// ============================================================
// 3. 安全
// ============================================================

data class SecuritySettings(
    val isSecureContentEnabled: Boolean = true,
    val isFlipToLockEnabled: Boolean = true,
    val isFlipExitAndClearStackEnabled: Boolean = false,
    val isLockOnBackground: Boolean = false,
    val lockTimeout: Long = 60000L,
    val isInvalidateBiometricKeyOnChange: Boolean = true
)

// ============================================================
// 4. 交互
// ============================================================

data class InteractionSettings(
    val isSwipeEnabled: Boolean = false,
    val swipeLeftAction: SwipeActionType = SwipeActionType.COPY_PASSWORD,
    val swipeRightAction: SwipeActionType = SwipeActionType.DETAIL,
    val autofillUiMode: AutofillUiMode = AutofillUiMode.SYSTEM_INLINE,
    val isAutoDownloadIcons: Boolean = true,
    val faviconDownloadWhitelist: Set<String> = emptySet()
)

// ============================================================
// 5. 保险库视图
// ============================================================

data class VaultViewSettings(
    val maxTabsWithoutScroll: Int = 4,
    val visibleTabs: VisibleTabsConfig? = null,
    val sort: VaultSortSpec = VaultSortSpec.DEFAULT,
    val entryCardPresentations: List<EntryCardPresentation> = emptyList()
)

data class VisibleTabsConfig(
    val tabKeys: Set<String>,
    val configured: Boolean = false
)

data class EntryCardPresentation(
    val entryTypeKey: String,
    val variantKey: String = "",
    val density: CardDensity = CardDensity.STANDARD,
    val showIcon: Boolean = true,
    val showFavorite: Boolean = true,
    val showSecondaryText: Boolean = true,
    val showQuickAction: Boolean = true
)

enum class CardDensity { COMPACT, STANDARD, COMFORTABLE }

// ============================================================
// 6. 备份
// ============================================================

data class BackupSettings(
    val directoryTreeUri: String? = null,
    val defaultExportFormat: ExportFormat = ExportFormat.ENCRYPTED,
    val includeIcons: Boolean = true,
    val includeAttachments: Boolean = true,
    val includeDeletedEntries: Boolean = true,
    val includedEntryTypes: Set<String> = emptySet(),
    val defaultImportMode: ImportMode = ImportMode.APPEND
)

enum class ExportFormat { ENCRYPTED, CSV, JSON }
enum class ImportMode { APPEND, REPLACE, MERGE }
