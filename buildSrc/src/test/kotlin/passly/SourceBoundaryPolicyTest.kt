package passly

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SourceBoundaryPolicyTest {
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
            assertEquals("PRESENTATION_UI_IMPORT", violations.single().ruleId)
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
        assertTrue(formatted.contains("[PRESENTATION_UI_IMPORT]"))
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
        assertEquals("PRESENTATION_FEATURE_DATA_IMPORT", violation.ruleId)
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
            "DATABASE_RECOVERY_DATA_ADAPTER_ONLY",
            SourceBoundaryVerifier.verify(
                listOf(presentation),
                SourceBoundaryPolicy.generalRules,
            ).single().ruleId,
        )
    }

    @Test
    fun featureImplementationCannotImportPresentation() {
        val source = EditorSource(
            path = "app/src/main/java/com/aozijx/passly/feature/backup/BackupUseCase.kt",
            content = "import com.aozijx.passly.presentation.feature.backup.BackupUiState",
        )

        assertEquals(
            "FEATURE_PRESENTATION_IMPORT",
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
            "BACKUP_FEATURE_DATA_IMPORT",
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
                "RUNTIME_SESSION_RESOURCE_NEUTRALITY",
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
            listOf("FEATURE_PRESENTATION_IMPORT", "FEATURE_PRESENTATION_IMPORT"),
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
}
