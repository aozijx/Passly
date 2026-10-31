package passly

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SourceBoundaryPolicyTest {
    @Test
    fun unregisteredFeatureBusinessCannotImportPresentationIncludingAliasesAndWildcards() {
        for (imported in listOf(
            "com.aozijx.passly.feature.sync_engine2.presentation.SyncUiState",
            "com.aozijx.passly.feature.sync_engine2.presentation.SyncUiState as UiState",
            "com.aozijx.passly.feature.sync_engine2.presentation.*",
        )) {
            val source = EditorSource(
                path = "app/src/main/java/com/aozijx/passly/feature/sync_engine2/SyncUseCase.kt",
                content = "import $imported",
            )
            val violation = SourceBoundaryVerifier.verify(listOf(source), SourceBoundaryPolicy.layerRules).single()
            assertEquals("LAYER_FEATURE", violation.ruleId)
            assertEquals(source.content, violation.evidence)
        }
    }

    @Test
    fun unregisteredFeaturePresentationCanUseSharedUiOnUnixAndWindowsPaths() {
        for (path in listOf(
            "app/src/main/java/com/aozijx/passly/feature/sync_engine2/presentation/SyncRoute.kt",
            "D:\\Passly\\app\\src\\main\\java\\com\\aozijx\\passly\\feature\\sync_engine2\\presentation\\SyncRoute.kt",
            "app/src/main/java/COM/AOZIJX/PASSLY/FEATURE/sync_engine2/PRESENTATION/SyncRoute.kt",
        )) {
            val source = EditorSource(path, "import com.aozijx.passly.presentation.shared.components.Card")
            assertEquals(emptyList(), SourceBoundaryVerifier.verify(listOf(source), SourceBoundaryPolicy.layerRules))
        }
    }

    @Test
    fun unregisteredFeaturePresentationStillRejectsDataWithPresentationOwnership() {
        val source = EditorSource(
            path = "app/src/main/java/com/aozijx/passly/feature/sync_engine2/presentation/SyncRoute.kt",
            content = "import com.aozijx.passly.data.repository.Repository",
        )
        val violation = SourceBoundaryVerifier.verify(listOf(source), SourceBoundaryPolicy.layerRules).single()
        assertEquals("LAYER_FEATURE_PRESENTATION", violation.ruleId)
        assertEquals("feature.presentation", violation.owner)
    }

    @Test
    fun presentationLookalikesAreNeitherUiExemptionsNorForbiddenUiImports() {
        val source = EditorSource(
            path = "app/src/main/java/com/aozijx/passly/feature/sync_engine2/presentation_models/Model.kt",
            content = "import com.aozijx.passly.feature.sync_engine2.presentation_models.Model",
        )
        assertEquals(emptyList(), SourceBoundaryVerifier.verify(listOf(source), SourceBoundaryPolicy.layerRules))
        val reverseImport = source.copy(content = "import com.aozijx.passly.feature.sync_engine2.presentation.SyncUiState")
        assertEquals("LAYER_FEATURE", SourceBoundaryVerifier.verify(listOf(reverseImport), SourceBoundaryPolicy.layerRules).single().ruleId)
    }

    @Test
    fun backupFeatureCannotImportSettingsOwnedUi() {
        val source = EditorSource(
            path = "app/src/main/java/com/aozijx/passly/feature/backup/presentation/BackupRoute.kt",
            content = "import com.aozijx.passly.presentation.ui.settings.backup.BackupSheet",
        )

        assertEquals(
            "BACKUP_FEATURE_SETTINGS_UI_OWNERSHIP",
            SourceBoundaryVerifier.verify(
                listOf(source),
                SourceBoundaryPolicy.generalRules,
            ).single().ruleId,
        )
    }

    @Test
    fun verticalFeatureRootAllowsOwnedUiButPresentationRootStaysClassified() {
        val featureUi = EditorSource(
            path = "app/src/main/java/com/aozijx/passly/feature/settings/presentation/SettingsScreen.kt",
            content = "import androidx.compose.runtime.Composable",
        )
        val unclassifiedPresentation = EditorSource(
            path = "app/src/main/java/com/aozijx/passly/presentation/vault/editor/Editor.kt",
            content = "package com.aozijx.passly.presentation.vault.editor",
        )

        assertEquals(
            emptyList(),
            SourceBoundaryVerifier.verify(listOf(featureUi), SourceBoundaryPolicy.layerRules),
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
                    "com.aozijx.passly.feature.vault.presentation.VaultUiState",
                    "com.aozijx.passly.feature.vault.VaultEntryPageSource",
                    "com.aozijx.passly.domain.entry.model.Entry",
                    "com.aozijx.passly.domain.entry.model.EntryType",
                    "com.aozijx.passly.data.repository.EntryRepositoryImpl",
                    "com.aozijx.passly.security.dek.DekManager",
                ),
            ),
            LayerCase(
                owner = "feature.presentation",
                path = "app/src/main/java/com/aozijx/passly/feature/vault/presentation/VaultViewModel.kt",
                allowed = listOf("com.aozijx.passly.feature.vault.VaultEntryPageSource", "com.aozijx.passly.domain.entry.model.Entry"),
                forbidden = listOf("com.aozijx.passly.data.repository.entry.RoomEntryQueryRepository"),
            ),
            LayerCase(
                owner = "presentation.shared",
                path = "app/src/main/java/com/aozijx/passly/presentation/shared/components/Card.kt",
                allowed = listOf(
                    "androidx.compose.runtime.Composable",
                    "com.aozijx.passly.core.ui.theme.PasslyTheme",
                    "com.aozijx.passly.domain.entry.model.EntryType",
                ),
                forbidden = listOf(
                    "com.aozijx.passly.feature.vault.presentation.VaultUiState",
                    "com.aozijx.passly.feature.vault.VaultEntryPageSource",
                    "com.aozijx.passly.domain.entry.model.Entry",
                    "com.aozijx.passly.domain.entry.model.EntryTypeMetadata",
                    "com.aozijx.passly.data.repository.EntryRepositoryImpl",
                    "com.aozijx.passly.security.dek.DekManager",
                ),
            ),
            LayerCase(
                owner = "feature",
                path = "app/src/main/java/com/aozijx/passly/feature/vault/VaultUseCase.kt",
                allowed = listOf(
                    "androidx.compose.runtime.Composable",
                    "com.aozijx.passly.domain.entry.model.Entry",
                    "com.aozijx.passly.core.error.result.AppResult",
                    "com.aozijx.passly.presentation.shared.error.toUiMessage",
                ),
                forbidden = listOf("com.aozijx.passly.feature.vault.presentation.VaultUiState"),
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
                    "com.aozijx.passly.feature.vault.presentation.VaultUiState",
                    "com.aozijx.passly.security.dek.DekManager",
                ),
            ),
            LayerCase(
                owner = "data",
                path = "data/src/main/java/com/aozijx/passly/data/repository/Repository.kt",
                allowed = listOf("androidx.room.Room", "com.aozijx.passly.domain.entry.model.Entry"),
                forbidden = listOf(
                    "com.aozijx.passly.app.PasslyApplication",
                    "com.aozijx.passly.feature.vault.VaultUseCase",
                    "com.aozijx.passly.feature.vault.presentation.VaultUiState",
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
                    "com.aozijx.passly.feature.vault.presentation.VaultUiState",
                ),
            ),
            LayerCase(
                owner = "security",
                path = "app/src/main/java/com/aozijx/passly/security/authentication/Auth.kt",
                allowed = listOf("android.content.Context", "com.aozijx.passly.app.session.DekSessionKeySource", "com.aozijx.passly.domain.access.port.AuthenticationManager"),
                forbidden = listOf(
                    "com.aozijx.passly.data.repository.EntryRepositoryImpl",
                    "com.aozijx.passly.feature.vault.VaultUseCase",
                    "com.aozijx.passly.feature.vault.presentation.VaultUiState",
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
                    "com.aozijx.passly.feature.vault.presentation.VaultUiState",
                    "com.aozijx.passly.security.dek.DekManager",
                ),
            ),
            LayerCase(
                owner = "app",
                path = "app/src/main/java/com/aozijx/passly/app/Composition.kt",
                allowed = listOf(
                    "com.aozijx.passly.data.repository.EntryRepositoryImpl",
                    "com.aozijx.passly.feature.vault.VaultUseCase",
                    "com.aozijx.passly.feature.vault.presentation.VaultUiState",
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
    fun presentationSharedRejectsFullyQualifiedDomainReferencesWithoutImports() {
        val source = EditorSource(
            path = "app/src/main/java/com/aozijx/passly/presentation/shared/components/Card.kt",
            content = "val entry: com.aozijx.passly.domain.entry.model.Entry? = null",
        )

        val violation = SourceBoundaryVerifier.verify(
            listOf(source),
            SourceBoundaryPolicy.layerRules,
        ).single()

        assertEquals("LAYER_PRESENTATION_SHARED", violation.ruleId)
        assertEquals(source.content, violation.evidence)
    }

    @Test
    fun fullyQualifiedReferencesHonorExactAllowlistAndIgnoreCommentsAndStrings() {
        val allowed = EditorSource(
            path = "app/src/main/java/com/aozijx/passly/presentation/shared/components/Card.kt",
            content = """
                // com.aozijx.passly.domain.entry.model.Entry
                val documentation = "com.aozijx.passly.domain.entry.model.Entry"
                /* com.aozijx.passly.domain.entry.model.Entry */
                val type: com.aozijx.passly.domain.entry.model.EntryType =
                    com.aozijx.passly.domain.entry.model.EntryType.LOGIN
            """.trimIndent(),
        )
        val sibling = allowed.copy(
            content = "val metadata: com.aozijx.passly.domain.entry.model.EntryTypeMetadata",
        )

        assertEquals(
            emptyList(),
            SourceBoundaryVerifier.verify(listOf(allowed), SourceBoundaryPolicy.layerRules),
        )
        assertEquals(
            "LAYER_PRESENTATION_SHARED",
            SourceBoundaryVerifier.verify(listOf(sibling), SourceBoundaryPolicy.layerRules).single().ruleId,
        )
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
            "com.aozijx.passly.feature.vault.presentation.list.VaultUiState",
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
            path = "app/src/main/java/com/aozijx/passly/feature/vault/presentation/list/VaultViewModel.kt",
            content = "import com.aozijx.passly.feature.vault.entry.VaultEntryPageSource",
        )
        val forbidden = allowed.copy(
            content = "import com.aozijx.passly.data.repository.entry.paging.EntryPagingStore",
        )

        assertEquals(
            emptyList(),
            SourceBoundaryVerifier.verify(listOf(allowed), SourceBoundaryPolicy.generalRules),
        )
        val violations = SourceBoundaryVerifier.verify(
            listOf(forbidden),
            SourceBoundaryPolicy.generalRules,
        )
        assertTrue(violations.any { it.ruleId == "LAYER_FEATURE_PRESENTATION" })
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
    fun featureImplementationCannotImportPresentation() {
        val source = EditorSource(
            path = "app/src/main/java/com/aozijx/passly/feature/backup/BackupUseCase.kt",
            content = "import com.aozijx.passly.feature.backup.presentation.BackupUiState",
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
    fun presentationOutsideUnlockCannotImportAuthenticationManager() {
        val source = EditorSource(
            path = "app/src/main/java/com/aozijx/passly/feature/shell/presentation/AppShellViewModel.kt",
            content = "import com.aozijx.passly.domain.access.port.AuthenticationManager",
        )

        assertEquals(
            "PRESENTATION_AUTH_MANAGER_IMPORT",
            SourceBoundaryVerifier.verify(
                listOf(source),
                SourceBoundaryPolicy.generalRules,
            ).single().ruleId,
        )
    }

    @Test
    fun applicationLifecycleCannotImportAuthenticationManager() {
        val source = EditorSource(
            path = "app/src/main/java/com/aozijx/passly/app/AppLifecycleObserver.kt",
            content = "import com.aozijx.passly.domain.access.port.AuthenticationManager",
        )

        assertEquals(
            "APPLICATION_AUTH_MANAGER_IMPORT",
            SourceBoundaryVerifier.verify(
                listOf(source),
                SourceBoundaryPolicy.generalRules,
            ).single().ruleId,
        )
    }

    @Test
    fun presentationCannotImportConcreteSessionController() {
        val source = EditorSource(
            path = "app/src/main/java/com/aozijx/passly/feature/shell/presentation/AppShellViewModel.kt",
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
    fun autofillCannotImportFullAuthenticationManager() {
        val source = EditorSource(
            path = "app/src/main/java/com/aozijx/passly/feature/autofill/core/AuthorizeAutofillInteraction.kt",
            content = "import com.aozijx.passly.domain.access.port.AuthenticationManager",
        )

        assertEquals(
            "AUTOFILL_AUTH_MANAGER_IMPORT",
            SourceBoundaryVerifier.verify(
                listOf(source),
                SourceBoundaryPolicy.generalRules,
            ).single().ruleId,
        )
    }

    @Test
    fun autofillPlatformAdaptersCannotImportRepositoriesOrAuthenticationPorts() {
        val sources = listOf(
            EditorSource(
                path = "app/src/main/java/com/aozijx/passly/feature/autofill/credential/service/Forbidden.kt",
                content = "import com.aozijx.passly.domain.entry.port.EntryQueryRepository",
            ),
            EditorSource(
                path = "app/src/main/java/com/aozijx/passly/feature/autofill/legacy/service/Forbidden.kt",
                content = "import com.aozijx.passly.domain.access.port.AuthenticationRequester",
            ),
        )

        assertEquals(
            listOf(
                "AUTOFILL_CREDENTIAL_ADAPTER_DOMAIN_PORT",
                "AUTOFILL_LEGACY_ADAPTER_DOMAIN_PORT",
            ),
            SourceBoundaryVerifier.verify(sources, SourceBoundaryPolicy.generalRules)
                .map { it.ruleId },
        )

        val readOnlySessionProjection = EditorSource(
            path = "app/src/main/java/com/aozijx/passly/feature/autofill/credential/CredentialManagerFlow.kt",
            content = "import com.aozijx.passly.domain.access.port.SecureSessionAccessState",
        )
        assertTrue(
            SourceBoundaryVerifier.verify(
                listOf(readOnlySessionProjection),
                SourceBoundaryPolicy.generalRules,
            ).isEmpty(),
        )
    }

    @Test
    fun autofillCandidateSummaryCannotContainAggregateOrSecretTypes() {
        val allowed = EditorSource(
            path = "domain/src/main/kotlin/com/aozijx/passly/domain/autofill/model/AutofillCandidateSummary.kt",
            content = "import com.aozijx.passly.domain.entry.model.query.EntryListItem",
        )
        val forbidden = listOf(
            "com.aozijx.passly.domain.entry.model.Entry",
            "com.aozijx.passly.domain.entry.model.EntrySecret",
            "com.aozijx.passly.domain.entry.model.credential.EntryCredential",
        ).map { importedType ->
            EditorSource(
                path = "domain/src/main/kotlin/com/aozijx/passly/domain/autofill/model/AutofillCandidateSummary.kt",
                content = "import $importedType",
            )
        }

        assertTrue(
            SourceBoundaryVerifier.verify(listOf(allowed), SourceBoundaryPolicy.generalRules).isEmpty(),
        )
        assertEquals(
            listOf(
                "AUTOFILL_CANDIDATE_SECRET_BOUNDARY",
                "AUTOFILL_CANDIDATE_SECRET_BOUNDARY",
                "AUTOFILL_CANDIDATE_SECRET_BOUNDARY",
            ),
            SourceBoundaryVerifier.verify(forbidden, SourceBoundaryPolicy.generalRules)
                .map { it.ruleId },
        )
    }

    @Test
    fun vaultUiCannotAcceptMultiplePagingStreams() {
        val source = EditorSource(
            path = "app/src/main/java/com/aozijx/passly/feature/vault/presentation/list/ui/VaultScreen.kt",
            content =
                "entryPages: Map<VaultAddTypeUiModel, Flow<PagingData<VaultListItemUiModel>>>",
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
    fun vaultEntryGridCannotDiscardItsExternalModifier() {
        val source = EditorSource(
            path = "app/src/main/java/com/aozijx/passly/feature/vault/presentation/list/ui/component/list/VaultEntryGrid.kt",
            content = "modifier = Modifier.fillMaxSize()",
        )

        assertEquals(
            "VAULT_LIST_EXTERNAL_MODIFIER",
            SourceBoundaryVerifier.verify(
                listOf(source),
                SourceBoundaryPolicy.generalRules,
            ).single().ruleId,
        )
    }

    @Test
    fun vaultPagingContainerCannotOwnEntryRowRendering() {
        val source = EditorSource(
            path = "app/src/main/java/com/aozijx/passly/feature/vault/presentation/list/ui/component/list/VaultEntryGrid.kt",
            content = "fun VaultEntryRow() = Unit",
        )

        assertEquals(
            "VAULT_LIST_ROW_OWNERSHIP",
            SourceBoundaryVerifier.verify(
                listOf(source),
                SourceBoundaryPolicy.generalRules,
            ).single().ruleId,
        )
    }

    @Test
    fun settingsNavigationCannotMirrorNavigatorDestinationInLocalState() {
        val source = EditorSource(
            path = "app/src/main/java/com/aozijx/passly/feature/settings/presentation/main/navigation/SettingsNavGraph.kt",
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
    fun unlockFeatureHostCannotRenderCredentialInputsDirectly() {
        val source = EditorSource(
            path = "app/src/main/java/com/aozijx/passly/feature/unlock/presentation/AuthenticationScreen.kt",
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
    fun passwordEditorRouteCannotRenderFieldsDirectly() {
        listOf(
            "NextFocusTextField(value = state.title)",
            "OutlinedTextField(value = state.title)",
        ).forEach { directField ->
            val source = EditorSource(
                path = "app/src/main/java/com/aozijx/passly/feature/vault/presentation/editor/password/AddPasswordEditorRoute.kt",
                content = directField,
            )

            assertEquals(
                "PASSWORD_EDITOR_ROUTE_PASSIVE_UI",
                SourceBoundaryVerifier.verify(
                    listOf(source),
                    SourceBoundaryPolicy.generalRules,
                ).single().ruleId,
            )
        }
    }

    @Test
    fun otpEditorFeatureCannotRenderOtpFormDirectly() {
        val source = EditorSource(
            path = "app/src/main/java/com/aozijx/passly/feature/vault/presentation/editor/otp/AddOtpEditorRoute.kt",
            content = "OtpConfigForm(state = state.form)",
        )

        assertEquals(
            "OTP_EDITOR_ROUTE_PASSIVE_UI",
            SourceBoundaryVerifier.verify(
                listOf(source),
                SourceBoundaryPolicy.generalRules,
            ).single().ruleId,
        )
    }

    @Test
    fun bankCardEditorFeatureCannotRenderCardTypeDropdownDirectly() {
        val source = EditorSource(
            path = "app/src/main/java/com/aozijx/passly/feature/vault/presentation/editor/bankcard/AddBankCardEditorRoute.kt",
            content = "CardTypeDropdown(selected = state.cardType)",
        )

        assertEquals(
            "BANK_CARD_EDITOR_ROUTE_PASSIVE_UI",
            SourceBoundaryVerifier.verify(
                listOf(source),
                SourceBoundaryPolicy.generalRules,
            ).single().ruleId,
        )
    }

    @Test
    fun settingsUiCannotImportVaultOwnedUiModelsOrComponents() {
        val source = EditorSource(
            path = "app/src/main/java/com/aozijx/passly/feature/settings/presentation/ui/interaction/SwipeGestureSettingsSection.kt",
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
    fun applicationCodeCannotDependOnGlobalSettingsSnapshotOrCommandBus() {
        val sources = listOf(
            EditorSource(
                path = "app/src/main/java/com/aozijx/passly/feature/settings/presentation/main/SettingsViewModel.kt",
                content = "import com.aozijx.passly.domain.settings.model.SettingsCommand",
            ),
            EditorSource(
                path = "app/src/main/java/com/aozijx/passly/feature/shell/presentation/AppShellViewModel.kt",
                content = "import com.aozijx.passly.domain.settings.port.AppSettingsRepository",
            ),
        )

        assertEquals(
            setOf("APP_GLOBAL_SETTINGS_BUS"),
            SourceBoundaryVerifier.verify(
                sources,
                SourceBoundaryPolicy.generalRules,
            ).map { it.ruleId }.toSet(),
        )
    }

    @Test
    fun appearanceSettingsCannotDependOnGlobalSettingsSnapshotOrCommandBus() {
        val source = EditorSource(
            path = "app/src/main/java/com/aozijx/passly/feature/settings/presentation/appearance/AppearanceSettingsViewModel.kt",
            content = """
                import com.aozijx.passly.domain.settings.model.SettingsCommand
                import com.aozijx.passly.domain.settings.port.AppSettingsRepository
            """.trimIndent(),
        )

        assertEquals(
            "SETTINGS_APPEARANCE_NARROW_PORT",
            SourceBoundaryVerifier.verify(
                listOf(source),
                SourceBoundaryPolicy.generalRules,
            ).map { it.ruleId }.distinct().single(),
        )
    }

    @Test
    fun securitySettingsCannotDependOnGlobalSettingsSnapshotOrCommandBus() {
        val source = EditorSource(
            path = "app/src/main/java/com/aozijx/passly/feature/settings/presentation/security/PrivacySettingsViewModel.kt",
            content = """
                import com.aozijx.passly.domain.settings.model.SettingsCommand
                import com.aozijx.passly.domain.settings.port.AppSettingsRepository
            """.trimIndent(),
        )

        assertEquals(
            "SECURITY_SETTINGS_NARROW_PORT",
            SourceBoundaryVerifier.verify(
                listOf(source),
                SourceBoundaryPolicy.generalRules,
            ).map { it.ruleId }.distinct().single(),
        )
    }

    @Test
    fun securityRuntimeCannotDependOnGlobalSettingsSnapshot() {
        val source = EditorSource(
            path = "app/src/main/java/com/aozijx/passly/app/clipboard/ClipboardCopyController.kt",
            content = "import com.aozijx.passly.domain.settings.port.AppSettingsRepository",
        )

        assertEquals(
            "SECURITY_RUNTIME_NARROW_SOURCE",
            SourceBoundaryVerifier.verify(
                listOf(source),
                SourceBoundaryPolicy.generalRules,
            ).single().ruleId,
        )
    }

    @Test
    fun messageSettingsCannotDependOnGlobalSettingsSnapshotOrCommandBus() {
        val sources = listOf(
            EditorSource(
                path = "app/src/main/java/com/aozijx/passly/feature/settings/presentation/main/general/NotificationSettingsViewModel.kt",
                content = "import com.aozijx.passly.domain.settings.model.SettingsCommand",
            ),
            EditorSource(
                path = "app/src/main/java/com/aozijx/passly/app/message/runtime/DefaultMessageSettingsSnapshotProvider.kt",
                content = "import com.aozijx.passly.domain.settings.port.AppSettingsRepository",
            ),
        )

        assertEquals(
            setOf("MESSAGE_SETTINGS_NARROW_PORT"),
            SourceBoundaryVerifier.verify(
                sources,
                SourceBoundaryPolicy.generalRules,
            ).map { it.ruleId }.toSet(),
        )
    }

    @Test
    fun backupSettingsCannotDependOnGlobalSettingsSnapshotOrCommandBus() {
        val sources = listOf(
            EditorSource(
                path = "app/src/main/java/com/aozijx/passly/feature/settings/presentation/backup/DataManagementSettingsViewModel.kt",
                content = "import com.aozijx.passly.domain.settings.model.SettingsCommand",
            ),
            EditorSource(
                path = "app/src/main/java/com/aozijx/passly/feature/backup/internal/operation/BackupOperationInteractor.kt",
                content = "import com.aozijx.passly.domain.settings.port.AppSettingsRepository",
            ),
        )

        assertEquals(
            setOf("BACKUP_SETTINGS_NARROW_PORT"),
            SourceBoundaryVerifier.verify(
                sources,
                SourceBoundaryPolicy.generalRules,
            ).map { it.ruleId }.toSet(),
        )
    }

    @Test
    fun vaultListCannotDependOnGlobalSettingsSnapshotOrCommandBus() {
        val source = EditorSource(
            path = "app/src/main/java/com/aozijx/passly/feature/vault/presentation/list/VaultViewModel.kt",
            content = """
                import com.aozijx.passly.domain.settings.model.SettingsCommand
                import com.aozijx.passly.domain.settings.port.AppSettingsRepository
            """.trimIndent(),
        )

        assertEquals(
            "VAULT_LIBRARY_SETTINGS_NARROW_PORT",
            SourceBoundaryVerifier.verify(
                listOf(source),
                SourceBoundaryPolicy.generalRules,
            ).map { it.ruleId }.distinct().single(),
        )
    }

    @Test
    fun interactionConsumersCannotDependOnGlobalSettingsSnapshotOrCommandBus() {
        val sources = listOf(
            EditorSource(
                path = "app/src/main/java/com/aozijx/passly/feature/settings/presentation/autofill/AutofillSettingsViewModel.kt",
                content = "import com.aozijx.passly.domain.settings.model.SettingsCommand",
            ),
            EditorSource(
                path = "app/src/main/java/com/aozijx/passly/feature/autofill/core/DiscoverAutofillCandidates.kt",
                content = "import com.aozijx.passly.domain.settings.port.AppSettingsRepository",
            ),
            EditorSource(
                path = "app/src/main/java/com/aozijx/passly/feature/vault/presentation/list/display/VaultDisplayViewModel.kt",
                content = "import com.aozijx.passly.domain.settings.port.AppSettingsRepository",
            ),
        )

        assertEquals(
            setOf("INTERACTION_SETTINGS_NARROW_PORT"),
            SourceBoundaryVerifier.verify(
                sources,
                SourceBoundaryPolicy.generalRules,
            ).map { it.ruleId }.toSet(),
        )
    }

    @Test
    fun dataManagementSettingsCannotOwnTrashStateOrCommands() {
        val source = EditorSource(
            path = "app/src/main/java/com/aozijx/passly/feature/settings/presentation/backup/DataManagementSettingsViewModel.kt",
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
    fun settingsCannotOwnDatabaseLifecycleOrResetCapability() {
        val source = EditorSource(
            path = "app/src/main/java/com/aozijx/passly/feature/settings/presentation/main/SettingsViewModel.kt",
            content = """
                import com.aozijx.passly.app.database.DatabaseLifecycleGateway
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
    fun retiredDatabaseRecoveryCannotReturn() {
        val source = EditorSource(
            path = "app/src/main/java/com/aozijx/passly/feature/database/recovery/Recovery.kt",
            content = "class DatabaseRecoveryStore",
        )

        assertEquals(
            "RETIRED_DATABASE_RECOVERY",
            SourceBoundaryVerifier.verify(
                listOf(source),
                SourceBoundaryPolicy.generalRules,
            ).single().ruleId,
        )
    }

    @Test
    fun runtimeSessionRejectsPlatformPersistenceAndPresentationImports() {
        val forbiddenImports = listOf(
            "android.content.Context",
            "androidx.room.RoomDatabase",
            "com.aozijx.passly.data.local.database.PasslyDatabase",
            "com.aozijx.passly.security.dek.DekManager",
            "com.aozijx.passly.feature.shell.presentation.AppShell",
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
            "com.aozijx.passly.core.observability.Logger",
            "com.aozijx.passly.core.platform.VaultResourcePaths",
            "com.aozijx.passly.core.telemetry.TelemetryReporter",
            "com.aozijx.passly.security.dek.FieldKeyManager",
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
                content = "import com.aozijx.passly.feature.autofill.presentation.legacy.AutofillFillActivity",
            ),
            EditorSource(
                path = "app/src/main/java/com/aozijx/passly/feature/autofill/credential/service/CredentialPendingIntentFactory.kt",
                content = "import com.aozijx.passly.feature.autofill.presentation.credential.CredentialResponseActivity",
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
            path = "app/src/main/java/com/aozijx/passly/feature/shell/presentation/navigation/PasslyNavHost.kt",
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

    @Test
    fun retiredEntryFieldKeysCannotReturn() {
        val source = EditorSource(
            path = "domain/src/main/kotlin/com/aozijx/passly/domain/entry/model/FieldKey.kt",
            content = "enum class FieldKey { TITLE, URIS }",
        )

        assertEquals(
            "RETIRED_ENTRY_FIELD_KEY",
            SourceBoundaryVerifier.verify(
                listOf(source),
                SourceBoundaryPolicy.generalRules,
            ).single().ruleId,
        )
    }

    @Test
    fun vaultBootstrapCapabilitiesCannotHaveSilentDefaults() {
        val source = EditorSource(
            path = "domain/src/main/kotlin/com/aozijx/passly/domain/access/port/VaultBootstrapStore.kt",
            content = "suspend fun loadAttachmentKeyEnvelope(): ByteArray? = null",
        )

        assertEquals(
            "VAULT_BOOTSTRAP_SILENT_DEFAULT",
            SourceBoundaryVerifier.verify(
                listOf(source),
                SourceBoundaryPolicy.generalRules,
            ).single().ruleId,
        )
    }

    @Test
    fun applicationCannotImportSecurityImplementation() {
        val source = EditorSource(
            path = "app/src/main/java/com/aozijx/passly/app/PasslyApplication.kt",
            content =
                "import com.aozijx.passly.security.authentication.BiometricRotationReconciler",
        )

        assertEquals(
            "APPLICATION_SECURITY_IMPLEMENTATION_IMPORT",
            SourceBoundaryVerifier.verify(
                listOf(source),
                SourceBoundaryPolicy.generalRules,
            ).single().ruleId,
        )
    }

    @Test
    fun appAdaptersCannotImportSecurityImplementation() {
        val sources = listOf(
            EditorSource(
                path = "app/src/main/java/com/aozijx/passly/app/session/DekSessionKeySource.kt",
                content = "import com.aozijx.passly.security.dek.DekManager",
            ),
            EditorSource(
                path = "app/src/main/java/com/aozijx/passly/app/database/backup/RoomBackupSnapshotReader.kt",
                content = "import com.aozijx.passly.security.dek.AttachmentContentCrypto",
            ),
        )

        assertEquals(
            listOf(
                "APP_ADAPTER_SECURITY_IMPLEMENTATION_IMPORT",
                "APP_ADAPTER_SECURITY_IMPLEMENTATION_IMPORT",
            ),
            SourceBoundaryVerifier.verify(sources, SourceBoundaryPolicy.generalRules)
                .map { it.ruleId },
        )
    }

    @Test
    fun applicationCannotToggleManifestServicesAtRuntime() {
        val source = EditorSource(
            path = "app/src/main/java/com/aozijx/passly/app/PasslyApplication.kt",
            content = "packageManager.setComponentEnabledSetting(component, state, flags)",
        )

        assertEquals(
            "APPLICATION_RUNTIME_SERVICE_TOGGLE",
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
