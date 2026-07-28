package com.aozijx.passly.domain.settings.model

import com.aozijx.passly.domain.notice.model.AppMessageSettings
import java.util.Locale

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
    val fontFamily: FontFamilyMode = FontFamilyMode.APP_BUNDLED,
    val isExpressive: Boolean = true
)

enum class ThemeMode { SYSTEM, LIGHT, DARK }
enum class FallbackPalette { BLUE, GREEN, RED, PURPLE, ORANGE, TEAL, PINK }
enum class AppLanguage(val locale: Locale?) {
    SYSTEM(null),
    ZH(Locale.SIMPLIFIED_CHINESE),
    EN(Locale.ENGLISH),
    JA(Locale.JAPANESE);

    /** BCP-47 标签；跟随系统时 AppCompat 需要空标签。 */
    val applicationLocaleTags: String
        get() = locale?.toLanguageTag().orEmpty()

    /** DataStore 使用的稳定值，兼容已经保存的 "system"。 */
    val storageTag: String
        get() = locale?.toLanguageTag() ?: SYSTEM_STORAGE_TAG

    companion object {
        private const val SYSTEM_STORAGE_TAG = "system"

        fun fromLanguageTag(tag: String): AppLanguage {
            if (tag.isBlank() || tag.equals(SYSTEM_STORAGE_TAG, ignoreCase = true)) {
                return SYSTEM
            }

            val requested = Locale.forLanguageTag(tag)
            return entries.firstOrNull { language ->
                val supported = language.locale ?: return@firstOrNull false
                val hasCompatibleRegion =
                    supported.country.isBlank() ||
                            requested.country.isBlank() ||
                            supported.country == requested.country
                supported.toLanguageTag().equals(tag, ignoreCase = true) ||
                        (supported.language == requested.language && hasCompatibleRegion)
            } ?: SYSTEM
        }
    }
}
enum class FontFamilyMode { SYSTEM, APP_BUNDLED }

// ============================================================
// 2. 界面行为
// ============================================================

data class InterfaceSettings(
    val hideSystemBars: Boolean = false,
    val collapseTopBarOnScroll: Boolean = false,
    val collapseTabBarOnScroll: Boolean = false,
    val outerCornerRadiusDp: Float = InterfaceStyleConstraints.DEFAULT_OUTER_RADIUS_DP,
    val innerCornerRadiusDp: Float = InterfaceStyleConstraints.DEFAULT_INNER_RADIUS_DP,
    val groupItemSpacingDp: Float = InterfaceStyleConstraints.DEFAULT_ITEM_SPACING_DP,
    val groupContentPaddingDp: Float = InterfaceStyleConstraints.DEFAULT_CONTENT_PADDING_DP
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
    val autofill: AutofillSettings = AutofillSettings(),
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
