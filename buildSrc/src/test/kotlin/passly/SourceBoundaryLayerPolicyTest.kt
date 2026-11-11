package passly

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SourceBoundaryLayerPolicyTest {
    @Test
    fun entryAndAccessPackagesCannotDependOnAutofill() {
        val entryImport =
            EditorSource(
                path = "/domain/src/main/kotlin/com/aozijx/passly/domain/entry/port/EntryCreation.kt",
                content = "import com.aozijx.passly.domain.autofill.model.AutofillInboxDraftId",
            )
        val accessImport =
            EditorSource(
                path = "/domain/src/main/kotlin/com/aozijx/passly/domain/access/port/InboxAccess.kt",
                content = "import com.aozijx.passly.domain.autofill.model.AutofillInboxStoredRecord",
            )
        val entryQualifiedReference =
            entryImport.copy(content = "val id = com.aozijx.passly.domain.autofill.model.AutofillInboxDraftId")
        val accessQualifiedReference =
            accessImport.copy(content = "val record = com.aozijx.passly.domain.autofill.model.AutofillInboxStoredRecord")
        val entryDecoys =
            entryImport.copy(
                content =
                    """
                    // com.aozijx.passly.domain.autofill.model.CommentOnly
                    val text = "com.aozijx.passly.domain.autofill.model.StringOnly"
                    val lookalike = com.aozijx.passly.domain.autofillish.model.Unrelated
                    """.trimIndent(),
            )

        assertEquals(
            "DOMAIN_ENTRY_NO_AUTOFILL",
            SourceBoundaryVerifier.verify(listOf(entryImport), SourceBoundaryPolicy.layerRules).singleOrNull()?.ruleId,
        )
        assertEquals(
            "DOMAIN_ACCESS_NO_AUTOFILL",
            SourceBoundaryVerifier.verify(listOf(accessImport), SourceBoundaryPolicy.layerRules).singleOrNull()?.ruleId,
        )
        assertEquals(
            "DOMAIN_ENTRY_NO_AUTOFILL",
            SourceBoundaryVerifier.verify(listOf(entryQualifiedReference), SourceBoundaryPolicy.layerRules).singleOrNull()?.ruleId,
        )
        assertEquals(
            "DOMAIN_ACCESS_NO_AUTOFILL",
            SourceBoundaryVerifier.verify(listOf(accessQualifiedReference), SourceBoundaryPolicy.layerRules).singleOrNull()?.ruleId,
        )
        assertEquals(emptyList(), SourceBoundaryVerifier.verify(listOf(entryDecoys), SourceBoundaryPolicy.layerRules))
    }

    @Test
    fun unregisteredFeatureBusinessCannotImportPresentationIncludingAliasesAndWildcards() {
        for (imported in listOf(
            "com.aozijx.passly.feature.sync_engine2.presentation.SyncUiState",
            "com.aozijx.passly.feature.sync_engine2.presentation.SyncUiState as UiState",
            "com.aozijx.passly.feature.sync_engine2.presentation.*",
        )) {
            val source =
                EditorSource(
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
        val source =
            EditorSource(
                path = "app/src/main/java/com/aozijx/passly/feature/sync_engine2/presentation/SyncRoute.kt",
                content = "import com.aozijx.passly.data.repository.Repository",
            )
        val violation = SourceBoundaryVerifier.verify(listOf(source), SourceBoundaryPolicy.layerRules).single()
        assertEquals("LAYER_FEATURE_PRESENTATION", violation.ruleId)
        assertEquals("feature.presentation", violation.owner)
    }

    @Test
    fun presentationLookalikesAreNeitherUiExemptionsNorForbiddenUiImports() {
        val source =
            EditorSource(
                path = "app/src/main/java/com/aozijx/passly/feature/sync_engine2/presentation_models/Model.kt",
                content = "import com.aozijx.passly.feature.sync_engine2.presentation_models.Model",
            )
        assertEquals(emptyList(), SourceBoundaryVerifier.verify(listOf(source), SourceBoundaryPolicy.layerRules))
        val reverseImport = source.copy(content = "import com.aozijx.passly.feature.sync_engine2.presentation.SyncUiState")
        assertEquals("LAYER_FEATURE", SourceBoundaryVerifier.verify(listOf(reverseImport), SourceBoundaryPolicy.layerRules).single().ruleId)
    }

    @Test
    fun backupFeatureCannotImportSettingsOwnedUi() {
        val source =
            EditorSource(
                path = "app/src/main/java/com/aozijx/passly/feature/backup/presentation/BackupOperationRoute.kt",
                content = "import com.aozijx.passly.presentation.ui.settings.backup.BackupSheet",
            )

        assertEquals(
            "BACKUP_FEATURE_SETTINGS_UI_OWNERSHIP",
            SourceBoundaryVerifier
                .verify(
                    listOf(source),
                    SourceBoundaryPolicy.generalRules,
                ).single()
                .ruleId,
        )
    }

    @Test
    fun backupPresentationUsesFeatureContractInsteadOfInternalImplementation() {
        val productionViolation =
            EditorSource(
                path = "app/src/main/java/com/aozijx/passly/feature/backup/presentation/BackupViewModel.kt",
                content = "import com.aozijx.passly.feature.backup.internal.operation.BackupOperationInteractor",
            )
        val testViolation =
            EditorSource(
                path = "app/src/test/java/com/aozijx/passly/feature/backup/presentation/BackupViewModelAccessTest.kt",
                content = "import com.aozijx.passly.feature.backup.internal.operation.BackupOperationInteractor",
            )
        val contract =
            EditorSource(
                path = productionViolation.path,
                content = "import com.aozijx.passly.feature.backup.contract.BackupOperationGateway",
            )

        assertEquals(
            "BACKUP_PRESENTATION_INTERNAL_DEPENDENCY",
            SourceBoundaryVerifier.verify(listOf(productionViolation), SourceBoundaryPolicy.generalRules).single().ruleId,
        )
        assertEquals(
            "BACKUP_PRESENTATION_INTERNAL_DEPENDENCY",
            SourceBoundaryVerifier.verify(listOf(testViolation), SourceBoundaryPolicy.generalRules).single().ruleId,
        )
        assertEquals(emptyList(), SourceBoundaryVerifier.verify(listOf(contract), SourceBoundaryPolicy.generalRules))
    }

    @Test
    fun verticalFeatureRootAllowsOwnedUiButPresentationRootStaysClassified() {
        val featureUi =
            EditorSource(
                path = "app/src/main/java/com/aozijx/passly/feature/settings/presentation/main/navigation/SettingsRoute.kt",
                content = "import androidx.compose.runtime.Composable",
            )
        val unclassifiedPresentation =
            EditorSource(
                path = "app/src/main/java/com/aozijx/passly/presentation/vault/editor/Editor.kt",
                content = "package com.aozijx.passly.presentation.vault.editor",
            )

        assertEquals(
            emptyList(),
            SourceBoundaryVerifier.verify(listOf(featureUi), SourceBoundaryPolicy.layerRules),
        )
        assertEquals(
            "LAYER_PRESENTATION_ROOT",
            SourceBoundaryVerifier
                .verify(
                    listOf(unclassifiedPresentation),
                    SourceBoundaryPolicy.layerRules,
                ).single()
                .ruleId,
        )
    }

    @Test
    fun packageLayerMatrixAllowsOnlyReviewedEdges() {
        val cases =
            listOf(
                LayerCase(
                    owner = "presentation.ui",
                    path = "app/src/main/java/com/aozijx/passly/presentation/ui/Screen.kt",
                    allowed = listOf("androidx.compose.runtime.Composable", "com.aozijx.passly.core.ui.theme.PasslyTheme"),
                    forbidden =
                        listOf(
                            "com.aozijx.passly.feature.vault.presentation.VaultUiState",
                            "com.aozijx.passly.feature.vault.model.AddType",
                            "com.aozijx.passly.domain.entry.port.EntryPagingRepository",
                            "com.aozijx.passly.domain.entry.model.Entry",
                            "com.aozijx.passly.domain.entry.model.EntryType",
                            "com.aozijx.passly.data.repository.EntryRepositoryImpl",
                            "com.aozijx.passly.security.dek.DekManager",
                        ),
                ),
                LayerCase(
                    owner = "feature.presentation",
                    path = "app/src/main/java/com/aozijx/passly/feature/vault/presentation/list/VaultViewModel.kt",
                    allowed =
                        listOf(
                            "com.aozijx.passly.domain.entry.port.EntryPagingRepository",
                            "com.aozijx.passly.domain.entry.model.Entry",
                        ),
                    forbidden = listOf("com.aozijx.passly.data.repository.entry.RoomEntryQueryRepository"),
                ),
                LayerCase(
                    owner = "presentation.shared",
                    path = "app/src/main/java/com/aozijx/passly/presentation/shared/components/Card.kt",
                    allowed =
                        listOf(
                            "androidx.compose.runtime.Composable",
                            "com.aozijx.passly.core.ui.theme.PasslyTheme",
                            "com.aozijx.passly.domain.entry.model.EntryType",
                        ),
                    forbidden =
                        listOf(
                            "com.aozijx.passly.feature.vault.presentation.VaultUiState",
                            "com.aozijx.passly.domain.entry.port.EntryPagingRepository",
                            "com.aozijx.passly.domain.entry.model.Entry",
                            "com.aozijx.passly.domain.entry.model.EntryTypeMetadata",
                            "com.aozijx.passly.data.repository.EntryRepositoryImpl",
                            "com.aozijx.passly.security.dek.DekManager",
                        ),
                ),
                LayerCase(
                    owner = "feature",
                    path = "app/src/main/java/com/aozijx/passly/feature/vault/detail/ExportEntryQrUseCase.kt",
                    allowed =
                        listOf(
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
                    forbidden =
                        listOf(
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
                    forbidden =
                        listOf(
                            "com.aozijx.passly.app.PasslyApplication",
                            "com.aozijx.passly.feature.vault.VaultUseCase",
                            "com.aozijx.passly.feature.vault.presentation.VaultUiState",
                        ),
                ),
                LayerCase(
                    owner = "core",
                    path = "core/src/main/kotlin/com/aozijx/passly/core/platform/Platform.kt",
                    allowed =
                        listOf(
                            "android.content.Context",
                            "com.aozijx.passly.domain.autofill.port.ApplicationLabelResolver",
                            "com.aozijx.passly.security.dek.DekManager",
                        ),
                    forbidden =
                        listOf(
                            "com.aozijx.passly.app.PasslyApplication",
                            "com.aozijx.passly.data.repository.EntryRepositoryImpl",
                            "com.aozijx.passly.feature.vault.VaultUseCase",
                            "com.aozijx.passly.feature.vault.presentation.VaultUiState",
                        ),
                ),
                LayerCase(
                    owner = "security",
                    path = "app/src/main/java/com/aozijx/passly/security/authentication/Auth.kt",
                    allowed =
                        listOf(
                            "android.content.Context",
                            "com.aozijx.passly.app.session.DekSessionKeySource",
                            "com.aozijx.passly.domain.access.port.AuthenticationManager",
                        ),
                    forbidden =
                        listOf(
                            "com.aozijx.passly.data.repository.EntryRepositoryImpl",
                            "com.aozijx.passly.feature.vault.VaultUseCase",
                            "com.aozijx.passly.feature.vault.presentation.VaultUiState",
                        ),
                ),
                LayerCase(
                    owner = "runtime.session",
                    path = "runtime/session/src/main/kotlin/com/aozijx/passly/runtime/session/Manager.kt",
                    allowed = listOf("kotlinx.coroutines.flow.StateFlow", "com.aozijx.passly.domain.access.model.SessionState"),
                    forbidden =
                        listOf(
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
                    allowed =
                        listOf(
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
                val violation =
                    SourceBoundaryVerifier
                        .verify(
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

    private data class LayerCase(
        val owner: String,
        val path: String,
        val allowed: List<String>,
        val forbidden: List<String>,
    )
}
