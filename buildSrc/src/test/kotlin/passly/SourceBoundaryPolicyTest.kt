package passly

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SourceBoundaryPolicyTest {
    @Test
    fun retiredUiPackagesRemainCoveredByCurrentOwnershipRules() {
        val featureUi = EditorSource(
            path = "app/src/main/java/com/aozijx/passly/feature/settings/SettingsScreen.kt",
            content = "import androidx.compose.runtime.Composable",
        )
        val unclassifiedPresentation = EditorSource(
            path = "app/src/main/java/com/aozijx/passly/presentation/vault/editor/Editor.kt",
            content = "package com.aozijx.passly.presentation.vault.editor",
        )

        assertEquals(
            "LAYER_FEATURE",
            SourceBoundaryVerifier.verify(listOf(featureUi), SourceBoundaryPolicy.layerRules)
                .single().ruleId,
        )
        assertEquals(
            "LAYER_PRESENTATION_ROOT",
            SourceBoundaryVerifier.verify(
                listOf(unclassifiedPresentation),
                SourceBoundaryPolicy.layerRules,
            ).single().ruleId,
        )
    }

    @Test
    fun packageLayerMatrixAllowsOnlyReviewedEdges() {
        val cases = listOf(
            LayerCase(
                owner = "presentation.ui",
                path = "app/src/main/java/com/aozijx/passly/presentation/ui/Screen.kt",
                allowed = listOf("androidx.compose.runtime.Composable", "com.aozijx.passly.core.ui.theme.PasslyTheme"),
                forbidden = listOf(
                    "com.aozijx.passly.presentation.feature.vault.VaultUiState",
                    "com.aozijx.passly.feature.vault.VaultEntryPageSource",
                    "com.aozijx.passly.domain.entry.model.Entry",
                    "com.aozijx.passly.data.repository.EntryRepositoryImpl",
                    "com.aozijx.passly.security.dek.DekManager",
                ),
            ),
            LayerCase(
                owner = "presentation.feature",
                path = "app/src/main/java/com/aozijx/passly/presentation/feature/vault/VaultViewModel.kt",
                allowed = listOf("com.aozijx.passly.feature.vault.VaultEntryPageSource", "com.aozijx.passly.domain.entry.model.Entry"),
                forbidden = listOf("com.aozijx.passly.data.repository.entry.RoomEntryQueryRepository"),
            ),
            LayerCase(
                owner = "feature",
                path = "app/src/main/java/com/aozijx/passly/feature/vault/VaultUseCase.kt",
                allowed = listOf("com.aozijx.passly.domain.entry.model.Entry", "com.aozijx.passly.core.error.result.AppResult"),
                forbidden = listOf("com.aozijx.passly.presentation.feature.vault.VaultUiState"),
            ),
            LayerCase(
                owner = "domain",
                path = "domain/src/main/kotlin/com/aozijx/passly/domain/entry/Entry.kt",
                allowed = listOf("kotlin.time.Duration", "com.aozijx.passly.core.error.result.AppResult"),
                forbidden = listOf(
                    "android.content.Context",
                    "androidx.lifecycle.ViewModel",
                    "com.aozijx.passly.app.PasslyApplication",
                    "com.aozijx.passly.data.repository.EntryRepositoryImpl",
                    "com.aozijx.passly.feature.vault.VaultUseCase",
                    "com.aozijx.passly.presentation.feature.vault.VaultUiState",
                    "com.aozijx.passly.security.dek.DekManager",
                ),
            ),
            LayerCase(
                owner = "data",
                path = "data/src/main/java/com/aozijx/passly/data/repository/Repository.kt",
                allowed = listOf("androidx.room.Room", "com.aozijx.passly.domain.entry.model.Entry", "com.aozijx.passly.security.search.BlindIndexer"),
                forbidden = listOf(
                    "com.aozijx.passly.app.PasslyApplication",
                    "com.aozijx.passly.feature.vault.VaultUseCase",
                    "com.aozijx.passly.presentation.feature.vault.VaultUiState",
                ),
            ),
            LayerCase(
                owner = "core",
                path = "core/src/main/kotlin/com/aozijx/passly/core/platform/Platform.kt",
                allowed = listOf("android.content.Context", "com.aozijx.passly.domain.autofill.port.ApplicationLabelResolver", "com.aozijx.passly.security.dek.DekManager"),
                forbidden = listOf(
                    "com.aozijx.passly.app.PasslyApplication",
                    "com.aozijx.passly.data.repository.EntryRepositoryImpl",
                    "com.aozijx.passly.feature.vault.VaultUseCase",
                    "com.aozijx.passly.presentation.feature.vault.VaultUiState",
                ),
            ),
            LayerCase(
                owner = "security",
                path = "app/src/main/java/com/aozijx/passly/security/authentication/Auth.kt",
                allowed = listOf("android.content.Context", "com.aozijx.passly.app.session.DekSessionKeySource", "com.aozijx.passly.domain.access.port.AuthenticationManager"),
                forbidden = listOf(
                    "com.aozijx.passly.data.repository.EntryRepositoryImpl",
                    "com.aozijx.passly.feature.vault.VaultUseCase",
                    "com.aozijx.passly.presentation.feature.vault.VaultUiState",
                ),
            ),
            LayerCase(
                owner = "runtime.session",
                path = "runtime/session/src/main/kotlin/com/aozijx/passly/runtime/session/Manager.kt",
                allowed = listOf("kotlinx.coroutines.flow.StateFlow", "com.aozijx.passly.domain.access.model.SessionState"),
                forbidden = listOf(
                    "android.content.Context",
                    "androidx.room.RoomDatabase",
                    "com.aozijx.passly.app.PasslyApplication",
                    "com.aozijx.passly.core.crypto.CryptoEngine",
                    "com.aozijx.passly.data.local.database.AppDatabase",
                    "com.aozijx.passly.feature.vault.VaultUseCase",
                    "com.aozijx.passly.presentation.feature.vault.VaultUiState",
                    "com.aozijx.passly.security.dek.DekManager",
                ),
            ),
            LayerCase(
                owner = "app",
                path = "app/src/main/java/com/aozijx/passly/app/Composition.kt",
                allowed = listOf(
                    "com.aozijx.passly.data.repository.EntryRepositoryImpl",
                    "com.aozijx.passly.feature.vault.VaultUseCase",
                    "com.aozijx.passly.presentation.feature.vault.VaultUiState",
                    "com.aozijx.passly.security.dek.DekManager",
                ),
                forbidden = emptyList(),
            ),
        )

        cases.forEach { case ->
            case.allowed.forEach { imported ->
                assertEquals(
                    emptyList(),
                    SourceBoundaryVerifier.verify(
                        listOf(EditorSource(case.path, "import $imported")),
                        SourceBoundaryPolicy.layerRules,
                    ),
                    "${case.owner} should allow $imported",
                )
            }
            case.forbidden.forEach { imported ->
                val violation = SourceBoundaryVerifier.verify(
                    listOf(EditorSource(case.path, "import $imported")),
                    SourceBoundaryPolicy.layerRules,
                ).single()
                assertEquals("LAYER_${case.owner.uppercase().replace('.', '_')}", violation.ruleId)
                assertTrue(violation.format().contains("owner=${case.owner}"))
                assertTrue(violation.format().contains(case.path))
                assertTrue(violation.format().contains(imported))
            }
        }
    }

    @Test
    fun directDomainConsumersAreRequiredByTheModulePolicy() {
        val requiredEdges = setOf(
            edge(":app", ":domain"),
            edge(":data", ":domain"),
        )
        val actualEdges = setOf(edge(":app", ":core"), edge(":data", ":core"))

        assertEquals(
            requiredEdges,
            missingRequiredEdges(requiredEdges, actualEdges),
        )
        assertEquals(
            emptySet(),
            missingRequiredEdges(requiredEdges, actualEdges + requiredEdges),
        )
    }

    @Test
    fun presentationUiRejectsExactForbiddenImportPrefixes() {
        val cases = listOf(
            "com.aozijx.passly.presentation.feature.vault.list.VaultUiState",
            "com.aozijx.passly.feature.vault.model.AddType",
            "com.aozijx.passly.domain.entry.model.Entry",
            "com.aozijx.passly.data.repository.EntryRepositoryImpl",
        )

        cases.forEach { importedType ->
            val violations = SourceBoundaryVerifier.verify(
                sources = listOf(uiSource("import $importedType")),
                rules = SourceBoundaryPolicy.generalRules,
            )

            assertEquals(1, violations.size, importedType)
            assertEquals("LAYER_PRESENTATION_UI", violations.single().ruleId)
            assertEquals("import $importedType", violations.single().evidence)
        }
    }

    @Test
    fun presentationUiRejectsViewModelLookupWithExactEvidence() {
        val violations = SourceBoundaryVerifier.verify(
            sources = listOf(uiSource("val owner = hiltViewModel<VaultViewModel>()")),
            rules = SourceBoundaryPolicy.generalRules,
        )

        assertEquals(1, violations.size)
        assertEquals("PRESENTATION_UI_VIEW_MODEL", violations.single().ruleId)
        assertEquals("val owner = hiltViewModel<VaultViewModel>()", violations.single().evidence)
    }

    @Test
    fun commentsAndSimilarlyNamedPackagesDoNotMatchImportRules() {
        val violations = SourceBoundaryVerifier.verify(
            sources = listOf(
                uiSource(
                    """
                    // import com.aozijx.passly.data.repository.Hidden
                    import com.aozijx.passly.database.preview.EntryPreview
                    import com.aozijx.passly.core.ui.theme.PasslyTheme
                    """.trimIndent(),
                ),
            ),
            rules = SourceBoundaryPolicy.generalRules,
        )

        assertEquals(emptyList(), violations)
    }

    @Test
    fun formattedViolationIncludesRulePathAndEvidence() {
        val violation = SourceBoundaryVerifier.verify(
            sources = listOf(uiSource("import com.aozijx.passly.domain.entry.model.Entry")),
            rules = SourceBoundaryPolicy.generalRules,
        ).single()

        val formatted = violation.format()
        assertTrue(formatted.contains("[LAYER_PRESENTATION_UI]"))
        assertTrue(formatted.contains("presentation/ui/vault/list/VaultScreen.kt"))
        assertTrue(formatted.contains("import com.aozijx.passly.domain.entry.model.Entry"))
    }

    @Test
    fun presentationFeatureMayUseVaultContractButNotDataPaging() {
        val allowed = EditorSource(
            path = "app/src/main/java/com/aozijx/passly/presentation/feature/vault/list/VaultViewModel.kt",
            content = "import com.aozijx.passly.feature.vault.entry.VaultEntryPageSource",
        )
        val forbidden = allowed.copy(
            content = "import com.aozijx.passly.data.repository.entry.paging.EntryPagingStore",
        )

        assertEquals(
            emptyList(),
            SourceBoundaryVerifier.verify(listOf(allowed), SourceBoundaryPolicy.generalRules),
        )
        val violation = SourceBoundaryVerifier.verify(
            listOf(forbidden),
            SourceBoundaryPolicy.generalRules,
        ).single()
        assertEquals("LAYER_PRESENTATION_FEATURE", violation.ruleId)
    }

    @Test
    fun vaultDataTypesAreRestrictedToAppAdapters() {
        val dataImport = "import com.aozijx.passly.data.repository.entry.paging.EntryPagingStore"
        val adapter = EditorSource(
            path = "app/src/main/java/com/aozijx/passly/app/entry/paging/DataVaultEntryPageSource.kt",
            content = dataImport,
        )
        val unrelated = adapter.copy(
            path = "app/src/main/java/com/aozijx/passly/app/entry/Unrelated.kt",
        )

        assertEquals(
            emptyList(),
            SourceBoundaryVerifier.verify(listOf(adapter), SourceBoundaryPolicy.generalRules),
        )
        assertEquals(
            "VAULT_DATA_ADAPTER_ONLY",
            SourceBoundaryVerifier.verify(
                listOf(unrelated),
                SourceBoundaryPolicy.generalRules,
            ).single().ruleId,
        )
    }

    @Test
    fun databaseRecoveryDataTypesAreRestrictedToTheirAppAdapter() {
        val dataImport =
            "import com.aozijx.passly.data.local.database.port.DatabaseRecoveryRepository"
        val adapter = EditorSource(
            path = "app/src/main/java/com/aozijx/passly/app/database/recovery/DataDatabaseRecoveryGateway.kt",
            content = dataImport,
        )
        val presentation = adapter.copy(
            path = "app/src/main/java/com/aozijx/passly/presentation/feature/settings/backup/DatabaseRecoveryViewModel.kt",
        )

        assertEquals(
            emptyList(),
            SourceBoundaryVerifier.verify(listOf(adapter), SourceBoundaryPolicy.generalRules),
        )
        assertEquals(
            setOf("DATABASE_RECOVERY_DATA_ADAPTER_ONLY", "LAYER_PRESENTATION_FEATURE"),
            SourceBoundaryVerifier.verify(
                listOf(presentation),
                SourceBoundaryPolicy.generalRules,
            ).mapTo(linkedSetOf()) { it.ruleId },
        )
    }

    @Test
    fun featureImplementationCannotImportPresentation() {
        val source = EditorSource(
            path = "app/src/main/java/com/aozijx/passly/feature/backup/BackupUseCase.kt",
            content = "import com.aozijx.passly.presentation.feature.backup.BackupUiState",
        )

        assertEquals(
            "LAYER_FEATURE",
            SourceBoundaryVerifier.verify(
                listOf(source),
                SourceBoundaryPolicy.generalRules,
            ).single().ruleId,
        )
    }

    @Test
    fun backupFeatureCannotImportDataImplementation() {
        val source = EditorSource(
            path = "app/src/main/java/com/aozijx/passly/feature/backup/BackupSnapshot.kt",
            content = "import com.aozijx.passly.data.local.database.session.AppDatabaseSession",
        )

        assertEquals(
            "LAYER_FEATURE",
            SourceBoundaryVerifier.verify(
                listOf(source),
                SourceBoundaryPolicy.generalRules,
            ).single().ruleId,
        )
    }

    @Test
    fun presentationCannotImportConcreteSessionController() {
        val source = EditorSource(
            path = "app/src/main/java/com/aozijx/passly/presentation/feature/shell/AppShellViewModel.kt",
            content =
                "import com.aozijx.passly.security.authentication.VaultSessionController",
        )

        assertEquals(
            "PRESENTATION_SESSION_CONTROLLER_IMPORT",
            SourceBoundaryVerifier.verify(
                listOf(source),
                SourceBoundaryPolicy.generalRules,
            ).single().ruleId,
        )
    }

    @Test
    fun vaultUiCannotAcceptARecreatedPagingFlowFactory() {
        val source = EditorSource(
            path = "app/src/main/java/com/aozijx/passly/presentation/ui/vault/list/VaultScreen.kt",
            content =
                "entryPages: (VaultQuickFilterUiModel) -> Flow<PagingData<VaultListItemUiModel>>",
        )

        assertEquals(
            "VAULT_UI_PAGING_FACTORY",
            SourceBoundaryVerifier.verify(
                listOf(source),
                SourceBoundaryPolicy.generalRules,
            ).single().ruleId,
        )
    }

    @Test
    fun settingsNavigationCannotMirrorNavigatorDestinationInLocalState() {
        val source = EditorSource(
            path = "app/src/main/java/com/aozijx/passly/presentation/feature/settings/main/navigation/SettingsNavGraph.kt",
            content = "mutableStateOf(navigator.currentDestination?.contentKey)",
        )

        assertEquals(
            "SETTINGS_DUPLICATE_NAVIGATION_STATE",
            SourceBoundaryVerifier.verify(
                listOf(source),
                SourceBoundaryPolicy.generalRules,
            ).single().ruleId,
        )
    }

    @Test
    fun scannerFeatureCannotKeepAPassiveViewEntryPoint() {
        val source = EditorSource(
            path = "app/src/main/java/com/aozijx/passly/presentation/feature/scanner/ScannerView.kt",
            content = "fun ScannerView(onBarcodeDetected: (String) -> Unit)",
        )

        assertEquals(
            "PASSIVE_SCANNER_VIEW_IN_FEATURE",
            SourceBoundaryVerifier.verify(
                listOf(source),
                SourceBoundaryPolicy.generalRules,
            ).single().ruleId,
        )
    }

    @Test
    fun unlockFeatureHostCannotRenderCredentialInputsDirectly() {
        val source = EditorSource(
            path = "app/src/main/java/com/aozijx/passly/presentation/feature/unlock/AuthenticationScreen.kt",
            content = "InputActionButton(value = state.appPassword)",
        )

        assertEquals(
            "UNLOCK_FEATURE_PASSIVE_CONTENT",
            SourceBoundaryVerifier.verify(
                listOf(source),
                SourceBoundaryPolicy.generalRules,
            ).single().ruleId,
        )
    }

    @Test
    fun editorSharedUiCannotRemainInFeatureCommonPackage() {
        val source = EditorSource(
            path = "app/src/main/java/com/aozijx/passly/presentation/feature/vault/editor/common/AddEntryScaffold.kt",
            content = "@Composable fun AddEntryScaffold()",
        )

        assertEquals(
            "EDITOR_COMMON_UI_IN_FEATURE",
            SourceBoundaryVerifier.verify(
                listOf(source),
                SourceBoundaryPolicy.generalRules,
            ).single().ruleId,
        )
    }

    @Test
    fun passwordEditorFeatureHostCannotRenderFieldsDirectly() {
        val source = EditorSource(
            path = "app/src/main/java/com/aozijx/passly/presentation/feature/vault/editor/password/AddPasswordEditorHost.kt",
            content = "EntryTitleField(value = state.title)",
        )

        assertEquals(
            "PASSWORD_EDITOR_HOST_PASSIVE_UI",
            SourceBoundaryVerifier.verify(
                listOf(source),
                SourceBoundaryPolicy.generalRules,
            ).single().ruleId,
        )
    }

    @Test
    fun otpEditorFeatureCannotRenderOtpFormDirectly() {
        val source = EditorSource(
            path = "app/src/main/java/com/aozijx/passly/presentation/feature/vault/editor/otp/AddOtpEditorHost.kt",
            content = "OtpConfigForm(state = state.form)",
        )

        assertEquals(
            "OTP_EDITOR_HOST_PASSIVE_UI",
            SourceBoundaryVerifier.verify(
                listOf(source),
                SourceBoundaryPolicy.generalRules,
            ).single().ruleId,
        )
    }

    @Test
    fun bankCardEditorFeatureCannotRenderCardTypeDropdownDirectly() {
        val source = EditorSource(
            path = "app/src/main/java/com/aozijx/passly/presentation/feature/vault/editor/bankcard/AddBankCardEditorHost.kt",
            content = "CardTypeDropdown(selected = state.cardType)",
        )

        assertEquals(
            "BANK_CARD_EDITOR_HOST_PASSIVE_UI",
            SourceBoundaryVerifier.verify(
                listOf(source),
                SourceBoundaryPolicy.generalRules,
            ).single().ruleId,
        )
    }

    @Test
    fun settingsUiCannotImportVaultOwnedUiModelsOrComponents() {
        val source = EditorSource(
            path = "app/src/main/java/com/aozijx/passly/presentation/ui/settings/interaction/SwipeGestureSettingsSection.kt",
            content = "import com.aozijx.passly.presentation.ui.vault.list.model.VaultSwipeActionUiModel",
        )

        assertEquals(
            "SETTINGS_UI_VAULT_OWNERSHIP",
            SourceBoundaryVerifier.verify(
                listOf(source),
                SourceBoundaryPolicy.generalRules,
            ).single().ruleId,
        )
    }

    @Test
    fun dataManagementSettingsCannotOwnTrashStateOrCommands() {
        val source = EditorSource(
            path = "app/src/main/java/com/aozijx/passly/presentation/feature/settings/backup/DataManagementSettingsViewModel.kt",
            content = """
                import com.aozijx.passly.domain.entry.port.EntryCommandRepository
                val deletedEntries = emptyList<String>()
            """.trimIndent(),
        )

        assertEquals(
            "SETTINGS_DATA_MANAGEMENT_TRASH_OWNERSHIP",
            SourceBoundaryVerifier.verify(
                listOf(source),
                SourceBoundaryPolicy.generalRules,
            ).map { it.ruleId }.distinct().single(),
        )
    }

    @Test
    fun settingsCannotOwnDatabaseLifecycleOrRecoveryCapability() {
        val source = EditorSource(
            path = "app/src/main/java/com/aozijx/passly/presentation/feature/settings/main/SettingsViewModel.kt",
            content = """
                import com.aozijx.passly.app.database.DatabaseLifecycleUseCases
                val action = ClearDatabase
            """.trimIndent(),
        )

        assertEquals(
            "SETTINGS_DATABASE_CAPABILITY_OWNERSHIP",
            SourceBoundaryVerifier.verify(
                listOf(source),
                SourceBoundaryPolicy.generalRules,
            ).map { it.ruleId }.distinct().single(),
        )
    }

    @Test
    fun runtimeSessionRejectsPlatformPersistenceAndPresentationImports() {
        val forbiddenImports = listOf(
            "android.content.Context",
            "androidx.room.RoomDatabase",
            "com.aozijx.passly.data.local.database.PasslyDatabase",
            "com.aozijx.passly.security.dek.DekManager",
            "com.aozijx.passly.presentation.feature.shell.AppShell",
        )

        forbiddenImports.forEach { forbiddenImport ->
            val source = EditorSource(
                path = "runtime/session/src/main/kotlin/com/aozijx/passly/runtime/session/Invalid.kt",
                content = "import $forbiddenImport",
            )

            assertEquals(
                "LAYER_RUNTIME_SESSION",
                SourceBoundaryVerifier.verify(
                    listOf(source),
                    SourceBoundaryPolicy.generalRules,
                ).single().ruleId,
            )
        }
    }

    @Test
    fun dataCanOnlyImportReviewedCoreFamilies() {
        val allowedImports = listOf(
            "com.aozijx.passly.core.crypto.FieldEncryptor",
            "com.aozijx.passly.core.error.result.AppResult",
            "com.aozijx.passly.core.platform.VaultResourcePaths",
            "com.aozijx.passly.core.telemetry.TelemetryReporter",
            "com.aozijx.passly.security.dek.FieldKeyManager",
            "com.aozijx.passly.security.search.BlindIndexer",
        )
        val forbiddenImports = listOf(
            "com.aozijx.passly.core.ui.theme.PasslyTheme",
            "com.aozijx.passly.core.permission.RuntimePermission",
            "com.aozijx.passly.core.platform.packageinfo.InstalledAppCatalog",
        )

        allowedImports.forEach { allowedImport ->
            val source = EditorSource(
                path = "data/src/main/java/com/aozijx/passly/data/Allowed.kt",
                content = "import $allowedImport",
            )
            assertTrue(
                SourceBoundaryVerifier.verify(
                    listOf(source),
                    SourceBoundaryPolicy.generalRules,
                ).isEmpty(),
            )
        }
        forbiddenImports.forEach { forbiddenImport ->
            val source = EditorSource(
                path = "data/src/main/java/com/aozijx/passly/data/Forbidden.kt",
                content = "import $forbiddenImport",
            )
            assertEquals(
                "DATA_CORE_PACKAGE_ACCESS",
                SourceBoundaryVerifier.verify(
                    listOf(source),
                    SourceBoundaryPolicy.generalRules,
                ).single().ruleId,
            )
        }
    }

    @Test
    fun autofillPendingIntentFactoriesCannotImportPresentationActivities() {
        val sources = listOf(
            EditorSource(
                path = "app/src/main/java/com/aozijx/passly/feature/autofill/legacy/AutofillPendingIntentFactory.kt",
                content = "import com.aozijx.passly.presentation.feature.autofill.legacy.AutofillFillActivity",
            ),
            EditorSource(
                path = "app/src/main/java/com/aozijx/passly/feature/autofill/credential/service/CredentialPendingIntentFactory.kt",
                content = "import com.aozijx.passly.presentation.feature.autofill.credential.CredentialResponseActivity",
            ),
        )

        assertEquals(
            listOf("LAYER_FEATURE", "LAYER_FEATURE"),
            SourceBoundaryVerifier.verify(sources, SourceBoundaryPolicy.generalRules)
                .map { it.ruleId },
        )
    }

    @Test
    fun passlyNavHostCannotRegisterFeatureDestinationsDirectly() {
        val source = EditorSource(
            path = "app/src/main/java/com/aozijx/passly/presentation/feature/shell/navigation/PasslyNavHost.kt",
            content = "composable(AppRoute.Vault.route) { VaultHost() }",
        )

        assertEquals(
            "SHELL_NAV_HOST_FEATURE_REGISTRATION",
            SourceBoundaryVerifier.verify(
                listOf(source),
                SourceBoundaryPolicy.generalRules,
            ).single().ruleId,
        )
    }

    @Test
    fun authenticationHostCannotRemainInAppLocalCoreUi() {
        val source = EditorSource(
            path = "app/src/main/java/com/aozijx/passly/core/ui/components/auth/AuthenticationHost.kt",
            content = "fun AuthenticationHost() = Unit",
        )

        assertEquals(
            "APP_LOCAL_CORE_AUTH_HOST",
            SourceBoundaryVerifier.verify(
                listOf(source),
                SourceBoundaryPolicy.generalRules,
            ).single().ruleId,
        )
    }

    private fun uiSource(content: String) = EditorSource(
        path = "app/src/main/java/com/aozijx/passly/presentation/ui/vault/list/VaultScreen.kt",
        content = content,
    )

    private data class LayerCase(
        val owner: String,
        val path: String,
        val allowed: List<String>,
        val forbidden: List<String>,
    )
}
