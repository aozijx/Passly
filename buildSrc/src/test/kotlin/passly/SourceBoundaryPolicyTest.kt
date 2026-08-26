package passly

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SourceBoundaryPolicyTest {
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

    private fun uiSource(content: String) = EditorSource(
        path = "app/src/main/java/com/aozijx/passly/presentation/ui/vault/list/VaultScreen.kt",
        content = content,
    )
}
