package passly

internal object SourceBoundaryPolicy {
    val editorMapperForbiddenMarkers = linkedMapOf(
        "repository" to listOf(".repository.", ".port.EntryCommandRepository"),
        "UUID" to listOf("UuidCreator", "java.util.UUID"),
        "clock" to listOf("System.currentTimeMillis", "System.nanoTime", ".Clock"),
        "codec" to listOf("Codec", ".codec."),
        "crypto" to listOf(".crypto.", "Cipher"),
        "EntryLink" to listOf("EntryLink"),
    )

    val layerRules = listOf(
        SourceBoundaryRule(
            id = "LAYER_PRESENTATION_UI",
            owner = "presentation.ui",
            sourcePathContains = "/com/aozijx/passly/presentation/ui/",
            forbiddenImportPrefixes = setOf(
                "com.aozijx.passly.presentation.feature.",
                "com.aozijx.passly.feature.",
                "com.aozijx.passly.domain.",
                "com.aozijx.passly.data.",
                "com.aozijx.passly.security.",
            ),
            message = "imports a forbidden namespace",
        ),
        SourceBoundaryRule(
            id = "LAYER_PRESENTATION_FEATURE",
            owner = "presentation.feature",
            sourcePathContains = "/com/aozijx/passly/presentation/feature/",
            forbiddenImportPrefixes = setOf("com.aozijx.passly.data."),
            message = "imports a forbidden namespace",
        ),
        SourceBoundaryRule(
            id = "LAYER_PRESENTATION_ROOT",
            owner = "presentation",
            sourcePathContains = "/com/aozijx/passly/presentation/",
            allowedSourcePathContains = setOf(
                "/presentation/feature/",
                "/presentation/ui/",
            ),
            forbiddenContentMarkers = setOf("package com.aozijx.passly.presentation."),
            message = "presentation source must be classified as feature or ui",
        ),
        SourceBoundaryRule(
            id = "LAYER_FEATURE",
            owner = "feature",
            sourcePathContains = "/com/aozijx/passly/feature/",
            allowedSourcePathContains = setOf(
                "/feature/vault/entry/VaultDataAdaptersTest.kt",
                "/feature/autofill/platform/AutofillLaunchTargetFactoryTest.kt",
            ),
            forbiddenImportPrefixes = setOf(
                "androidx.compose.",
                "com.aozijx.passly.data.",
                "com.aozijx.passly.presentation.",
            ),
            message = "imports a forbidden namespace",
        ),
        SourceBoundaryRule(
            id = "LAYER_DOMAIN",
            owner = "domain",
            sourcePathContains = "/domain/src/",
            forbiddenImportPrefixes = setOf(
                "android.",
                "androidx.",
                "com.aozijx.passly.app.",
                "com.aozijx.passly.data.",
                "com.aozijx.passly.feature.",
                "com.aozijx.passly.presentation.",
                "com.aozijx.passly.security.",
            ),
            message = "imports a forbidden namespace",
        ),
        SourceBoundaryRule(
            id = "LAYER_DATA",
            owner = "data",
            sourcePathContains = "/data/src/",
            forbiddenImportPrefixes = setOf(
                "com.aozijx.passly.app.",
                "com.aozijx.passly.feature.",
                "com.aozijx.passly.presentation.",
            ),
            message = "imports a forbidden namespace",
        ),
        SourceBoundaryRule(
            id = "LAYER_CORE",
            owner = "core",
            sourcePathContains = "/core/src/",
            forbiddenImportPrefixes = setOf(
                "com.aozijx.passly.app.",
                "com.aozijx.passly.data.",
                "com.aozijx.passly.feature.",
                "com.aozijx.passly.presentation.",
            ),
            message = "imports a forbidden namespace",
        ),
        SourceBoundaryRule(
            id = "LAYER_CORE_COMMON",
            owner = "core.common",
            sourcePathContains = "/core/common/src/",
            forbiddenImportPrefixes = setOf(
                "android.",
                "androidx.",
                "com.aozijx.passly.app.",
                "com.aozijx.passly.data.",
                "com.aozijx.passly.feature.",
                "com.aozijx.passly.presentation.",
                "com.aozijx.passly.security.",
            ),
            message = "imports a forbidden namespace",
        ),
        SourceBoundaryRule(
            id = "LAYER_SECURITY",
            owner = "security",
            sourcePathContains = "/com/aozijx/passly/security/",
            allowedSourcePathContains = setOf(
                "/security/authentication/RecoveryModeBoundaryTest.kt",
            ),
            forbiddenImportPrefixes = setOf(
                "com.aozijx.passly.data.",
                "com.aozijx.passly.feature.",
                "com.aozijx.passly.presentation.",
            ),
            message = "imports a forbidden namespace",
        ),
        SourceBoundaryRule(
            id = "LAYER_RUNTIME_SESSION",
            owner = "runtime.session",
            sourcePathContains = "/runtime/session/src/",
            forbiddenImportPrefixes = setOf(
                "android.",
                "androidx.",
                "com.aozijx.passly.app.",
                "com.aozijx.passly.core.",
                "com.aozijx.passly.data.",
                "com.aozijx.passly.feature.",
                "com.aozijx.passly.presentation.",
                "com.aozijx.passly.security.",
                "dagger.",
                "javax.inject.",
                "net.sqlcipher.",
            ),
            message = "imports a forbidden namespace",
        ),
        SourceBoundaryRule(
            id = "LAYER_APP",
            owner = "app",
            sourcePathContains = "/com/aozijx/passly/app/",
            message = "imports a forbidden namespace",
        ),
    )

    val generalRules = layerRules + listOf(
        SourceBoundaryRule(
            id = "PRESENTATION_UI_VIEW_MODEL",
            sourcePathContains = "/presentation/ui/",
            forbiddenContentMarkers = setOf("ViewModel", "hiltViewModel", "viewModel("),
            message = "presentation UI cannot own or look up a ViewModel",
        ),
        SourceBoundaryRule(
            id = "PRESENTATION_DI_MODULE",
            sourcePathContains = "/presentation/",
            forbiddenContentMarkers = setOf("@Module"),
            message = "presentation cannot declare a dependency injection module",
        ),
        SourceBoundaryRule(
            id = "VAULT_DATA_ADAPTER_ONLY",
            sourcePathContains = "/app/src/",
            forbiddenImportPrefixes = setOf(
                "com.aozijx.passly.data.repository.entry.paging.EntryPagingStore",
                "com.aozijx.passly.data.local.database.port.EntryDataRefreshNotifier",
            ),
            allowedSourcePathContains = setOf(
                "/app/entry/paging/DataVaultEntryPageSource.kt",
                "/app/entry/paging/DataVaultDataChangeSignal.kt",
                "/presentation/feature/",
                "/feature/vault/entry/VaultDataAdaptersTest.kt",
            ),
            message = "vault paging Data types may only be imported by App adapters",
        ),
        SourceBoundaryRule(
            id = "DATABASE_RECOVERY_DATA_ADAPTER_ONLY",
            sourcePathContains = "/app/src/",
            forbiddenImportPrefixes = setOf(
                "com.aozijx.passly.data.local.database.model.DatabaseRecovery",
                "com.aozijx.passly.data.local.database.port.DatabaseRecoveryRepository",
            ),
            allowedSourcePathContains = setOf(
                "/app/database/recovery/DataDatabaseRecoveryGateway.kt",
                "/app/database/recovery/DataDatabaseRecoveryGatewayTest.kt",
            ),
            message = "database recovery Data types may only be imported by the App adapter",
        ),
        SourceBoundaryRule(
            id = "PRESENTATION_SESSION_CONTROLLER_IMPORT",
            sourcePathContains = "/presentation/",
            forbiddenImportPrefixes = setOf(
                "com.aozijx.passly.security.authentication.VaultSessionController",
            ),
            message = "presentation imports the concrete session controller",
        ),
        SourceBoundaryRule(
            id = "VAULT_UI_PAGING_FACTORY",
            sourcePathContains = "/presentation/ui/vault/list/",
            forbiddenContentMarkers = setOf(
                "entryPages: (VaultQuickFilterUiModel) -> Flow",
            ),
            message = "vault UI accepts a paging Flow factory with unstable identity",
        ),
        SourceBoundaryRule(
            id = "SETTINGS_DUPLICATE_NAVIGATION_STATE",
            sourcePathContains = "/SettingsNavGraph.kt",
            forbiddenContentMarkers = setOf(
                "mutableStateOf(navigator.currentDestination?.contentKey)",
            ),
            message = "settings mirrors navigator destination in local state",
        ),
        SourceBoundaryRule(
            id = "PASSIVE_SCANNER_VIEW_IN_FEATURE",
            sourcePathContains = "/presentation/feature/scanner/",
            forbiddenContentMarkers = setOf("fun ScannerView("),
            message = "scanner render entry point remains in presentation feature",
        ),
        SourceBoundaryRule(
            id = "UNLOCK_FEATURE_PASSIVE_CONTENT",
            sourcePathContains = "/presentation/feature/unlock/AuthenticationScreen.kt",
            forbiddenContentMarkers = setOf("InputActionButton("),
            message = "unlock feature host renders passive credential input content",
        ),
        SourceBoundaryRule(
            id = "EDITOR_COMMON_UI_IN_FEATURE",
            sourcePathContains = "/presentation/feature/vault/editor/common/",
            forbiddenContentMarkers = setOf("@Composable"),
            message = "shared passive editor UI remains in presentation feature",
        ),
        SourceBoundaryRule(
            id = "PASSWORD_EDITOR_HOST_PASSIVE_UI",
            sourcePathContains = "/presentation/feature/vault/editor/password/",
            forbiddenContentMarkers = setOf("PasslyOutlinedTextField("),
            message = "password editor feature host renders passive fields",
        ),
        SourceBoundaryRule(
            id = "OTP_EDITOR_HOST_PASSIVE_UI",
            sourcePathContains = "/presentation/feature/vault/editor/otp/",
            forbiddenContentMarkers = setOf("OtpConfigForm("),
            message = "OTP editor feature host renders passive form content",
        ),
        SourceBoundaryRule(
            id = "BANK_CARD_EDITOR_HOST_PASSIVE_UI",
            sourcePathContains = "/presentation/feature/vault/editor/bankcard/",
            forbiddenContentMarkers = setOf("CardTypeDropdown("),
            message = "bank card editor feature host renders passive form content",
        ),
        SourceBoundaryRule(
            id = "SETTINGS_UI_VAULT_OWNERSHIP",
            sourcePathContains = "/presentation/ui/settings/",
            forbiddenImportPrefixes = setOf("com.aozijx.passly.presentation.ui.vault.list.model."),
            message = "settings UI imports a Vault-owned list model",
        ),
        SourceBoundaryRule(
            id = "SETTINGS_DATA_MANAGEMENT_TRASH_OWNERSHIP",
            sourcePathContains = "/presentation/feature/settings/backup/DataManagementSettings",
            forbiddenImportPrefixes = setOf(
                "com.aozijx.passly.domain.entry.port.EntryCommandRepository",
                "com.aozijx.passly.domain.entry.port.EntryListQueryRepository",
                "com.aozijx.passly.presentation.ui.vault.list.trash.",
            ),
            forbiddenContentMarkers = setOf(
                "deletedEntries",
                "RestoreTrashEntry",
                "DeleteTrashEntry",
                "EmptyTrash",
            ),
            message = "data management settings owns Vault trash state or commands",
        ),
        SourceBoundaryRule(
            id = "SETTINGS_DATABASE_CAPABILITY_OWNERSHIP",
            sourcePathContains = "/presentation/feature/settings/",
            forbiddenImportPrefixes = setOf(
                "com.aozijx.passly.app.database.DatabaseLifecycleUseCases",
                "com.aozijx.passly.feature.database.recovery.DatabaseRecoveryGateway",
                "com.aozijx.passly.presentation.feature.database.recovery.DatabaseRecoveryViewModel",
            ),
            forbiddenContentMarkers = setOf(
                "ClearDatabase",
                "isClearingDatabase",
                "DatabaseClearStarted",
                "DatabaseClearFinished",
            ),
            message = "settings owns database lifecycle or recovery capability",
        ),
        SourceBoundaryRule(
            id = "DATA_CORE_PACKAGE_ACCESS",
            sourcePathContains = "/data/src/",
            forbiddenImportPrefixes = setOf("com.aozijx.passly.core."),
            allowedImportPrefixes = setOf(
                "com.aozijx.passly.core.crypto.",
                "com.aozijx.passly.core.error.",
                "com.aozijx.passly.core.platform.VaultResourcePaths",
                "com.aozijx.passly.core.telemetry.",
            ),
            message = "Data imports a Core package outside its reviewed persistence allowlist",
        ),
        SourceBoundaryRule(
            id = "SHELL_NAV_HOST_FEATURE_REGISTRATION",
            sourcePathContains = "/presentation/feature/shell/navigation/PasslyNavHost.kt",
            forbiddenContentMarkers = setOf("composable("),
            message = "shell NavHost registers a feature destination directly",
        ),
        SourceBoundaryRule(
            id = "APP_LOCAL_CORE_AUTH_HOST",
            sourcePathContains = "/app/src/main/java/com/aozijx/passly/core/ui/components/auth/",
            forbiddenContentMarkers = setOf("AuthenticationHost", "ActivityAuthUiHost"),
            message = "authentication host implementation remains in app-local core UI",
        ),
    )
}
